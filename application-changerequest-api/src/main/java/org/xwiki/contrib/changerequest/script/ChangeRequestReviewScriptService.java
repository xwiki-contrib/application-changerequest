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
        ChangeRequestReview review = null;
        UserReference userReference = this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE);
        ChangeRequestAuthorizationScriptService authorizationScriptService = null;
        try {
            authorizationScriptService = this.componentManagerProvider.get()
                .getInstance(ScriptService.class, "changerequest.authorization");
        } catch (ComponentLookupException e) {
            throw new ChangeRequestException("Error when trying to access authorization script service.", e);
        }
        if (authorizationScriptService.isAuthorizedToReview(changeRequest)) {
            review = this.changeRequestManager.addReview(changeRequest, userReference, approved);
        } else {
            logger.warn("Unauthorized user [{}] trying to add review to [{}].", userReference, changeRequest);
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
}
