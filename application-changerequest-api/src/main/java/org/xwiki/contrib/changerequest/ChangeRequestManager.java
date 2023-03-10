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
package org.xwiki.contrib.changerequest;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;
import org.xwiki.user.UserReference;

/**
 * Component responsible to perform business operations on change requests.
 *
 * @version $Id$
 * @since 0.1
 */
@Role
@Unstable
public interface ChangeRequestManager
{
    /**
     * Check if all conditions are met so that a change request can be merged.
     * This method checks in particular if there's no conflict in the change request, if it's status allows is to be
     * merged, and if the approval strategy is met.
     *
     * @param changeRequest the change request to check for merging.
     * @return {@code true} if the change request can be merged.
     * @throws ChangeRequestException in case of problems during one of the check.
     */
    boolean canBeMerged(ChangeRequest changeRequest) throws ChangeRequestException;

    /**
     * @return the {@link MergeApprovalStrategy} currently configured
     *          (see {@link ChangeRequestConfiguration#getMergeApprovalStrategy()}).
     * @throws ChangeRequestException in case of problem for getting the component.
     * @since 0.4
     */
    default MergeApprovalStrategy getMergeApprovalStrategy() throws ChangeRequestException
    {
        return null;
    }

    /**
     * Check if the given reference can be requested for deletion.
     * Right now we don't authorize to request for deletions pages that belongs to extensions or that are hidden, or
     * that contains xclasses.
     *
     * @param documentReference the reference to check if it can be requested for deletion.
     * @return {@code true} if the given reference can be requested for deletion.
     * @throws ChangeRequestException in case of problem when checking document properties.
     */
    default boolean canDeletionBeRequested(DocumentReference documentReference) throws ChangeRequestException
    {
        return false;
    }

    /**
     * Check if the given change request is ready for merging, and change its status accordingly.
     *
     * @param changeRequest the change request to be checked
     * @throws ChangeRequestException in case of problem during the checks.
     * @since 0.6
     */
    default void computeReadyForMergingStatus(ChangeRequest changeRequest) throws ChangeRequestException
    {
    }

    /**
     * Update the status of the given change request with the new status, only if it's not set yet.
     * This method also triggers {@link #computeReadyForMergingStatus(ChangeRequest)} after the status change and
     * triggers a {@link org.xwiki.contrib.changerequest.events.ChangeRequestStatusChangedEvent}.
     *
     * @param changeRequest the change request for which to change the status.
     * @param newStatus the new status to be set.
     * @throws ChangeRequestException in case of problem when saving the change request.
     * @since 0.6
     */
    default void updateStatus(ChangeRequest changeRequest, ChangeRequestStatus newStatus) throws ChangeRequestException
    {
    }

    /**
     * Add a new review to the given change request.
     *
     * @param changeRequest the change request on which to add a review.
     * @param approved {@code true} if it's an approval review.
     * @param reviewer the user who performed the review.
     * @return the newly created review.
     * @throws ChangeRequestException in case of problem when saving the review.
     */
    default ChangeRequestReview addReview(ChangeRequest changeRequest, UserReference reviewer, boolean approved)
        throws ChangeRequestException
    {
        return addReview(changeRequest, reviewer, approved, null);
    }

    /**
     * Add a new review to the given change request.
     *
     * @param changeRequest the change request on which to add a review.
     * @param approved {@code true} if it's an approval review.
     * @param reviewer the user who performed the review.
     * @param originalApprover the user on behalf of whom the review is performed, or {@code null}
     * @return the newly created review.
     * @throws ChangeRequestException in case of problem when saving the review.
     * @since 0.13
     */
    @Unstable
    ChangeRequestReview addReview(ChangeRequest changeRequest, UserReference reviewer, boolean approved,
        UserReference originalApprover) throws ChangeRequestException;

    /**
     * Define if the current file change is outdated and should be rebased.
     *
     * @param fileChange the file change for which to check if it is outdated.
     * @return {@code true} if the file change should be rebased.
     * @throws ChangeRequestException in case of problem when loading the document.
     * @since 0.9
     */
    default boolean isFileChangeOutdated(FileChange fileChange) throws ChangeRequestException
    {
        return false;
    }

    /**
     * Check if the given template provider is currently supported by change request in page creation.
     *
     * @param templateProviderReference reference of the template provider to check for support.
     * @return {@code true} if the given template provider is supported.
     * @throws ChangeRequestException in case of problem for loading information.
     * @since 0.9
     */
    default boolean isTemplateProviderSupported(DocumentReference templateProviderReference)
        throws ChangeRequestException
    {
        return false;
    }

    /**
     * Check if the given template is currently supported by change request in page creation.
     *
     * @param templateReference reference of the template to check for support.
     * @return {@code true} if the given template is supported.
     * @throws ChangeRequestException in case of problem for loading information.
     * @since 0.9
     */
    default boolean isTemplateSupported(DocumentReference templateReference)
        throws ChangeRequestException
    {
        return false;
    }

    /**
     * Perform a rebase operation on a whole change request.
     * This method calls a rebase on all latest filechanges of the change request and also trigger a
     * {@link org.xwiki.contrib.changerequest.events.ChangeRequestRebasedEvent}.
     *
     * @param changeRequest the change request to be rebased.
     * @throws ChangeRequestException in case of problem to perform the rebase operation.
     */
    default void rebase(ChangeRequest changeRequest) throws ChangeRequestException
    {
    }

    /**
     * Perform a rebase operation on a single filechange.
     * This method calls a rebase on the given filechange of the change request and also trigger a
     * {@link org.xwiki.contrib.changerequest.events.FileChangeRebasedEvent}.
     *
     * @param fileChange the filechange to be rebased.
     * @throws ChangeRequestException in case of problem to perform the rebase operation.
     */
    default void rebase(FileChange fileChange) throws ChangeRequestException
    {
    }

    /**
     * Invalidate all reviews of a change request. This method should be called whenever any content has been updated
     * in a change request.
     *
     * @param changeRequest the change request for which to invalidate reviews
     * @throws ChangeRequestException in case of problem for saving the reviews
     */
    default void invalidateReviews(ChangeRequest changeRequest) throws ChangeRequestException
    {
    }

    /**
     * Compute and return the title for the given change request and file change.
     * If it's not already in cache, this method will put the computed title in cache.
     *
     * @param changeRequestId the identifier of the change request for which to retrieve a document title
     * @param fileChangeId the identifier of the file change for which to retrieve a document title
     * @return a title or {@code null} if there was a problem to compute it
     */
    default String getTitle(String changeRequestId, String fileChangeId)
    {
        return null;
    }
}
