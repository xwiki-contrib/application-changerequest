/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.contrib.changerequest.script;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestManager;
import org.xwiki.contrib.changerequest.ChangeRequestReview;
import org.xwiki.contrib.changerequest.ChangeRequestStatus;
import org.xwiki.contrib.changerequest.DelegateApproverManager;
import org.xwiki.contrib.changerequest.ReviewInvalidationReason;
import org.xwiki.contrib.changerequest.storage.ReviewStorageManager;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

/**
 * Script service dedicated to handle reviews in change request.
 *
 * @version $Id$
 * @since 0.8
 */
@Component
@Named("changerequest.review")
@Singleton
@Unstable
public class ChangeRequestReviewScriptService implements ScriptService
{
    @Inject
    private ChangeRequestManager changeRequestManager;

    @Inject
    private UserReferenceResolver<CurrentUserReference> currentUserReferenceResolver;

    @Inject
    private ReviewStorageManager reviewStorageManager;

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    private DelegateApproverManager<ChangeRequest> changeRequestDelegateApproverManager;

    @Inject
    private Logger logger;

    /**
     * Allow to add a review to the given change request.
     *
     * @param changeRequest the change request for which to add a review.
     * @param approved {@code true} if the review is an approval, {@code false} if it requests changes.
     * @return {@code true} if the review has been properly stored.
     * @throws ChangeRequestException in case of problem for storing the review.
     */
    public ChangeRequestReview addReview(ChangeRequest changeRequest, boolean approved)
        throws ChangeRequestException
    {
        return this.addReview(changeRequest, approved, null);
    }

    /**
     * Create and save a review to the given change request and returns it.
     *
     * @param changeRequest the change request for which to create a review.
     * @param approved whether the review is an approval or not
     * @param originalApprover the reference of the user on behalf of whom the review is done, or {@code null}.
     * @return the review created
     * @throws ChangeRequestException in case of problem for saving the review.
     * @since 0.13
     */
    @Unstable
    public ChangeRequestReview addReview(ChangeRequest changeRequest, boolean approved, UserReference originalApprover)
        throws ChangeRequestException
    {
        ChangeRequestReview review = null;
        UserReference userReference = this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE);
        ChangeRequestAuthorizationScriptService authorizationScriptService = null;
        try {
            authorizationScriptService = this.componentManagerProvider.get()
                .getInstance(ScriptService.class, "changerequest.authorization");
        } catch (ComponentLookupException e) {
            throw new ChangeRequestException("Error when trying to access authorization script service.", e);
        }
        boolean isAuthorized;
        String loggerMessage;
        Object[] loggerParams;
        if (originalApprover != null && !originalApprover.equals(userReference)) {
            isAuthorized = authorizationScriptService.isAuthorizedToReviewOnBehalf(changeRequest, originalApprover);
            loggerMessage = "Unauthorized user [{}] trying to add review to [{}] on behalf of [{}].";
            loggerParams = new Object[] { userReference, changeRequest, originalApprover };
        } else {
            isAuthorized = authorizationScriptService.isAuthorizedToReview(changeRequest);
            loggerMessage = "Unauthorized user [{}] trying to add review to [{}].";
            loggerParams = new Object[] { userReference, changeRequest };
        }

        if (isAuthorized) {
            review = this.changeRequestManager.addReview(changeRequest, userReference, approved, originalApprover);
        } else {
            logger.warn(loggerMessage, loggerParams);
        }
        return review;
    }

    /**
     * Retrieve a review based on its identifier.
     *
     * @param changeRequest the change request for which to retrieve a review.
     * @param reviewId the id of the review to retrieve.
     * @return an {@link Optional#empty()} if the review cannot be found, else an optional containing the requested
     *          {@link ChangeRequestReview}.
     */
    public Optional<ChangeRequestReview> getReview(ChangeRequest changeRequest, String reviewId)
    {
        return changeRequest.getReviews().stream().filter(review -> reviewId.equals(review.getId())).findFirst();
    }

    /**
     * Change the validity of the review to mark it as invalid, or on contrary to make it valid again.
     *
     * @param review the review for which to change the validity status.
     * @param isValid the new validity status to set.
     * @return {@code true} if the review has been properly saved with the new status.
     * @throws ChangeRequestException in case of problem when saving the review.
     */
    public boolean setReviewValidity(ChangeRequestReview review, boolean isValid) throws ChangeRequestException
    {
        if (canEditReview(review)) {
            review.setValid(isValid);
            if (!isValid) {
                review.setReviewInvalidationReason(ReviewInvalidationReason.MANUAL);
            }
            review.setSaved(false);
            this.reviewStorageManager.save(review);
            this.changeRequestManager.computeReadyForMergingStatus(review.getChangeRequest());
            return true;
        }
        return false;
    }

    /**
     * Check if the current user can edit the given review.
     * @param review the review for which to check if it can be edited.
     * @return {@code true} if the review is authored by the current user and the change request is not merged yet.
     */
    public boolean canEditReview(ChangeRequestReview review)
    {
        ChangeRequestStatus status = review.getChangeRequest().getStatus();
        UserReference currentUserReference = this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE);
        return review.isLastFromAuthor() && status != ChangeRequestStatus.MERGED
            && review.getAuthor().equals(currentUserReference);
    }

    /**
     * Check if the given user already reviewed the given change request, and if the review is still valid.
     *
     * @param userReference the user who might have performed a review.
     * @param changeRequest the change request which might have been reviewed
     * @return {@code true} if a review has been performed by the given user on the given change request, and the review
     *         is still valid.
     */
    public boolean alreadyReviewed(UserReference userReference, ChangeRequest changeRequest)
    {
        return changeRequest.getReviews().stream()
            .anyMatch(changeRequestReview ->
                changeRequestReview.isValid()
                && (changeRequestReview.getAuthor().equals(userReference)
                ||  userReference.equals(changeRequestReview.getOriginalApprover())));
    }

    /**
     * Check if the given change request is already reviewed by the current user.
     *
     * @param changeRequest the change request which might have been reviewed
     * @return {@code true} if a review has been performed by the given user on the given change request, and the review
     *         is still valid.
     * @see #alreadyReviewed(UserReference, ChangeRequest)
     * @since 0.13
     */
    public boolean alreadyReviewed(ChangeRequest changeRequest)
    {
        UserReference currentUserReference = this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE);
        return alreadyReviewed(currentUserReference, changeRequest);
    }

    /**
     * Retrieve the set of approvers on behalf of whom the current user can review if the delegate mechanism is enabled.
     *
     * @param changeRequest the change request for which to retrieve the approvers
     * @return a set of user references corresponding to the approvers for whom the current user can perform a review
     * @throws ChangeRequestException in case of problem for retrieving the original approvers
     * @since 0.13
     */
    @Unstable
    public Set<UserReference> getOriginalApprovers(ChangeRequest changeRequest) throws ChangeRequestException
    {
        UserReference currentUserReference = this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE);
        return this.changeRequestDelegateApproverManager.getOriginalApprovers(currentUserReference, changeRequest);
    }

    /**
     * Check if the change request has valid reviews.
     * @param changeRequest the change request to check for valid reviews.
     * @return {@code true} if the change request has at least one valid review
     * @since 1.5
     */
    public boolean hasValidReviews(ChangeRequest changeRequest)
    {
        return changeRequest.getReviews().stream().anyMatch(ChangeRequestReview::isValid);
    }
}
