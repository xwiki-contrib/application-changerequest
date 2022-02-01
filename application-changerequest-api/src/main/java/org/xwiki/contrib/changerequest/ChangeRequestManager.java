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

import java.util.List;
import java.util.Optional;

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Role;
import org.xwiki.diff.ConflictDecision;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;
import org.xwiki.store.merge.MergeDocumentResult;
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
     * Check if the given file change expose conflicts with the current version of the documents.
     *
     * @param fileChange the change to be checked for conflicts.
     * @return {@code true} if it contains conflicts, {@code false} otherwise.
     * @throws ChangeRequestException in case of problem for detecting conflicts.
     */
    boolean hasConflicts(FileChange fileChange) throws ChangeRequestException;

    /**
     * Check if the given user is authorized to merge the given change request.
     *
     * @param userReference the user for which to check the authorizations
     * @param changeRequest the change request to check
     * @return {@code true} if the user has the appropriate rights to perform the merge.
     * @throws ChangeRequestException in case of problem when checking if the user is an approver.
     */
    boolean isAuthorizedToMerge(UserReference userReference, ChangeRequest changeRequest) throws ChangeRequestException;

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
     * Perform a merge without saving between the given filechange and latest published version of the document
     * and returns the {@link MergeDocumentResult} containing all needed information to see diff and/or handle
     * conflicts.
     *
     * @param fileChange the file change for which to perform a merge.
     * @return a {@link MergeDocumentResult} with information about the merge.
     * @throws ChangeRequestException in case of problem when retrieving the information.
     * @since 0.4
     */
    default ChangeRequestMergeDocumentResult getMergeDocumentResult(FileChange fileChange)
        throws ChangeRequestException
    {
        return null;
    }

    /**
     * Perform a merge and fix the conflicts with the provided decision. Note that this method lead to saving a new
     * file change with the conflict resolution.
     *
     * @param fileChange the file change for which to perform a merge.
     * @param resolutionChoice the global decision to make for fixing the conflicts.
     * @param conflictDecisionList the specific decisions to take for each conflict if
     *         {@link ConflictResolutionChoice#CUSTOM} was chosen.
     * @return {@code true} if the merge succeeded without creating any new conflicts, {@code false} if some conflicts
     *          remained.
     * @throws ChangeRequestException in case of error to perform the merge.
     * @since 0.4
     */
    default boolean mergeWithConflictDecision(FileChange fileChange, ConflictResolutionChoice resolutionChoice,
        List<ConflictDecision<?>> conflictDecisionList) throws ChangeRequestException
    {
        return false;
    }

    /**
     * Merge a given modified document in the given change request, without saving the result.
     *
     * @param modifiedDocument a document with changes not yet saved.
     * @param previousVersion the version of the document where the modifications have been started.
     * @param changeRequest an existing change request.
     * @return an empty optional if the change request did not contain any changes related to the given document, else
     *          returns an optional containing the result of the merge: this one can be checked for conflicts.
     * @throws ChangeRequestException in case of problem for detecting conflicts.
     * @since 0.3
     */
    default Optional<MergeDocumentResult> mergeDocumentChanges(DocumentModelBridge modifiedDocument,
        String previousVersion, ChangeRequest changeRequest)
        throws ChangeRequestException
    {
        return Optional.empty();
    }

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
     * Check if the given user is authorized to review the given change request.
     *
     * @param userReference the user for which to check authorizations.
     * @param changeRequest the change request to review.
     * @return {@code true} if the user is not one of the change request author and it authorized to review it.
     * @throws ChangeRequestException in case of problem when checking if a user is an approver.
     * @since 0.4
     */
    default boolean isAuthorizedToReview(UserReference userReference, ChangeRequest changeRequest)
        throws ChangeRequestException
    {
        return false;
    }

    /**
     * Check if the given user is authorized to fix a conflict related to the given file change.
     * @param userReference the user for which to check authorization.
     * @param fileChange the file change concerned by the conflict.
     * @return {@code true} if the user is authorized to fix the conflict.
     * @since 0.4
     */
    default boolean isAuthorizedToFixConflict(UserReference userReference, FileChange fileChange)
    {
        return false;
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
     * Define if the given user is authorized to edit the given change request. An edition can be either changing the
     * status, modifying the description, or even rebasing the change request.
     * Only change request that are not merged can be edited, and only authors or administrators of the change request
     * document are authorized to do it.
     * @param userReference the user for which to check rights.
     * @param changeRequest the change request to check if it can be edited.
     * @return {@code true} if the given user can edit the change request, {@code false otherwise}.
     * @since 0.7
     */
    default boolean isAuthorizedToEdit(UserReference userReference, ChangeRequest changeRequest)
    {
        return false;
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
        return null;
    }

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
}
