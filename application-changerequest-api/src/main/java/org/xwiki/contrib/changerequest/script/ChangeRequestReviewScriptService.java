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
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestManager;
import org.xwiki.contrib.changerequest.ChangeRequestReview;
import org.xwiki.contrib.changerequest.events.ChangeRequestReviewAddedEvent;
import org.xwiki.contrib.changerequest.storage.ReviewStorageManager;
import org.xwiki.observation.ObservationManager;
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
    private ObservationManager observationManager;

    @Inject
    private Logger logger;

    /**
     * Allow to add a review to the given change request.
     *
     * @param changeRequest the change request for which to add a review.
     * @param approved {@code true} if the review is an approval, {@code false} if it requests changes.
     * @return {@code true} if the review has been properly stored.
     */
    public ChangeRequestReview addReview(ChangeRequest changeRequest, boolean approved)
        throws ChangeRequestException
    {
        ChangeRequestReview review = null;
        UserReference userReference = this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE);
        if (this.isAuthorizedToReview(changeRequest)) {
            review = new ChangeRequestReview(changeRequest, approved, userReference);
            this.reviewStorageManager.save(review);
            changeRequest.addReview(review);
            this.observationManager.notify(new ChangeRequestReviewAddedEvent(), changeRequest.getId(), review);
            this.changeRequestManager.computeReadyForMergingStatus(changeRequest);
        } else {
            logger.warn("Unauthorized user [{}] trying to add review to [{}].", userReference, changeRequest);
        }
        return review;
    }

    /**
     * Check if the current user is authorized to review the given change request.
     *
     * @param changeRequest the change request about to be reviewed.
     * @return {@code true} if the change request can be reviewed by current user.
     */
    public boolean isAuthorizedToReview(ChangeRequest changeRequest) throws ChangeRequestException
    {
        UserReference currentUserReference = this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE);
        return this.changeRequestManager.isAuthorizedToReview(currentUserReference, changeRequest);
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
     * @return {@code true} if the review is authored by the current user.
     */
    public boolean canEditReview(ChangeRequestReview review)
    {
        UserReference currentUserReference = this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE);
        return review.getAuthor().equals(currentUserReference);
    }
}
