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
import java.util.Set;

import org.xwiki.component.annotation.Role;
import org.xwiki.contrib.rights.SecurityRuleDiff;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;
import org.xwiki.user.UserReference;

/**
 * Component responsible to handle the rights synchronization between change requests and modified documents, and to
 * perform different rights checks for change request.
 *
 * @version $Id$
 * @since 0.7
 */
@Unstable
@Role
public interface ChangeRequestRightsManager
{
    /**
     * Copy all rights coming from the original change request to the target one, except view rights rules.
     *
     * @param originalChangeRequest the change request from which to get the rights to copy.
     * @param targetChangeRequest the change request where to copy the rights.
     * @throws ChangeRequestException in case of problems when reading or writing the rights.
     */
    void copyAllButViewRights(ChangeRequest originalChangeRequest, ChangeRequest targetChangeRequest)
        throws ChangeRequestException;

    /**
     * Determine if view access is still consistent if a new change related to the given document reference is added
     * to the given change request. The consistency here means that there's no rules that are contradictory between
     * the documents included in the change request, and the new document.
     *
     * @param changeRequest the change request where the new filechange should be added.
     * @param newChange the change to be added.
     * @return {@code true} if the given document reference can be added to the change request without creating right
     *          inconsistency.
     * @throws ChangeRequestException in case of problem to access the rights.
     */
    boolean isViewAccessConsistent(ChangeRequest changeRequest, DocumentReference newChange)
        throws ChangeRequestException;

    /**
     * Determine if the view right is still consistent in the change request for the given list of user references.
     * The consistency here consists only in defining if each user or group has independently same allow or deny access
     * of each document of the change request. This method should be called whenever a right is updated in a document.
     *
     * @param changeRequest the change request for which to check the consistency.
     * @param subjectReferences the users or groups for which to check the consistency of rights.
     * @return {@code true} if the view right is consistent across all document of the change request for each given
     *          user independently.
     * @throws ChangeRequestException in case of problem to access the rights.
     */
    boolean isViewAccessStillConsistent(ChangeRequest changeRequest, Set<DocumentReference> subjectReferences)
        throws ChangeRequestException;

    /**
     * Copy the view right rules coming from the given document reference to the change request.
     * Note that all inherited rules are copied too.
     *
     * @param changeRequest the change request in which to copy rights.
     * @param newChange the reference from which to get rights to copy.
     * @throws ChangeRequestException in case of problem for accessing or copying rights.
     */
    void copyViewRights(ChangeRequest changeRequest, EntityReference newChange) throws ChangeRequestException;

    /**
     * Apply the provided right changes to the change request.
     *
     * @param changeRequest the change request on which to apply the right changes.
     * @param ruleDiffList a list of diff changes of rights.
     * @throws ChangeRequestException in case of problem when applying the changes.
     */
    void applyChanges(ChangeRequest changeRequest, List<SecurityRuleDiff> ruleDiffList) throws ChangeRequestException;

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
     * Check if the given user is authorized to review the given change request.
     *
     * @param userReference the user for which to check authorizations.
     * @param changeRequest the change request to review.
     * @return {@code true} if the user is not one of the change request author and it authorized to review it.
     * @throws ChangeRequestException in case of problem when checking if a user is an approver.
     */
    boolean isAuthorizedToReview(UserReference userReference, ChangeRequest changeRequest)
        throws ChangeRequestException;

    /**
     * Check if the given user is authorized to perform a review on behalf of the original approver, on the given change
     * request.
     * This should only returns {@code true} if the delegate approver mechanism is enabled, the original approver is an
     * explicit approver and the given user is a delegate approver of them.
     *
     * @param userReference the user for which to check the authorization
     * @param changeRequest the change request for which to check the authorization
     * @param originalApprover the user on behalf of whom the authorization might be given
     * @return {@code true} if the delegate mechanism is enabled and the given user  is not an author, and is a delegate
     *          of the original approver who is also an approver of the given change request.
     * @throws ChangeRequestException in case of problem to resolve the delegate approvers.
     * @since 0.13
     */
    @Unstable
    boolean isAuthorizedToReviewOnBehalf(UserReference userReference, ChangeRequest changeRequest,
        UserReference originalApprover) throws ChangeRequestException;

    /**
     * Check if the given user is authorized to perform a review as a delegate of one of the approver of the given
     * change request.
     * This should only returns {@code true} if the delegate mechanism is enabled, the change request has explicit
     * approvers, and  the given user is a delegate of at least one of them. Moreover, this cannot return {@code true}
     * if the given user is an author of the change request.
     *
     * @param userReference the user for which to check the authorization
     * @param changeRequest the change request for which to check the authorization
     * @return {@code true} if the delegate mechanism is enabled, the change request has explicit
     *         approvers, the given user is not an author of the change request, and is a delegate of at least one of
     *         the approvers.
     * @throws ChangeRequestException in case of problem to resolve the delegate approvers.
     * @since 0.13
     */
    @Unstable
    boolean isAuthorizedToReviewAsDelegate(UserReference userReference, ChangeRequest changeRequest)
        throws ChangeRequestException;

    /**
     * Check if an user is authorized to comment a change request. This method should always check first if the user
     * is authorized to review (see {@link #isAuthorizedToReview(UserReference, ChangeRequest)}) and allow to comment if
     * that's the case. Then it should fallback on checking if the user has comment right.
     *
     * @param userReference the user for which to check rights.
     * @param changeRequest the change request for which to check if the user can comment.
     * @return {@code true} if the user is authorized to post a comment.
     * @throws ChangeRequestException in case of problem when checking if a user is an approver.
     */
    boolean isAuthorizedToComment(UserReference userReference, ChangeRequest changeRequest)
        throws ChangeRequestException;

    /**
     * Define if the given user is authorized to edit the given change request. An edition can be either changing the
     * status, modifying the description, or even rebasing the change request.
     * Only change request that are opened can be edited, and only authors or administrators of the change
     * request document are authorized to do it.
     * @param userReference the user for which to check rights.
     * @param changeRequest the change request to check if it can be edited.
     * @return {@code true} if the given user can edit the change request, {@code false otherwise}.
     */
    boolean isAuthorizedToEdit(UserReference userReference, ChangeRequest changeRequest);

    /**
     * Define if the given user is authorized to re-open a closed change request. This method basically performs the
     * same checks as {@link #isAuthorizedToEdit(UserReference, ChangeRequest)} except that it always returns false for
     * merged change requests.
     * @param userReference the user for which to check rights.
     * @param changeRequest the change request to check if it can be re-opened.
     * @return {@code true} if the given user can re-open the change request, {@code false otherwise}.
     */
    boolean isAuthorizedToOpen(UserReference userReference, ChangeRequest changeRequest);

    /**
     * Check if the given user is authorized to perform a split of the change request.
     * Users that are authors of the change request, and admins are authorized to split a change request.
     *
     * @param userReference the user for which to check rights
     * @param changeRequest the change request that is requested to be split
     * @return {@code true} if the given user is an author or an admin
     */
    default boolean isAuthorizedToSplit(UserReference userReference, ChangeRequest changeRequest)
    {
        return false;
    }

    /**
     * Check if the given user is allowed to use change request to edit the given document reference.
     * @param userReference the user for whom to perform the check
     * @param documentReference the reference for which to check authorization
     * @return {@code true} if the given user is allowed to perform the edition with change request
     * @throws ChangeRequestException in case of problem when performing the check
     * @since 1.10
     */
    default boolean isEditWithChangeRequestAllowed(UserReference userReference, DocumentReference documentReference)
        throws ChangeRequestException
    {
        return false;
    }

    /**
     * Check if the given user is allowed to use change request to create a document having the given reference for
     * parent.
     * @param userReference the user for whom to perform the check
     * @param parentSpaceReference the parent space where the new document would be created
     * @return {@code true} if the given user is allowed to create a document with change request at the given place
     * @throws ChangeRequestException in case of problem when performing the check
     * @since 1.10
     */
    default boolean isCreateWithChangeRequestAllowed(UserReference userReference,
        DocumentReference parentSpaceReference) throws ChangeRequestException
    {
        return false;
    }
}
