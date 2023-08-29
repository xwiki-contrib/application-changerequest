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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestRightsManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.stability.Unstable;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

/**
 * Script service dedicated to perform authorization check for change request.
 *
 * @version $Id$
 * @since 0.11
 */

@Component
@Named("changerequest.authorization")
@Singleton
@Unstable
public class ChangeRequestAuthorizationScriptService implements ScriptService
{
    @Inject
    private ChangeRequestRightsManager changeRequestRightsManager;

    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    private UserReferenceResolver<CurrentUserReference> currentUserReferenceResolver;

    /**
     * Check if the current user can edit the change request.
     *
     * @param changeRequest the change request for which to check the authors.
     * @return {@code true} if the current user is one of the author of the given change request and the change request
     *          is not merged or closed yet.
     * @since 0.7
     */
    public boolean isAuthorizedToEdit(ChangeRequest changeRequest)
    {
        UserReference currentUser = this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE);
        return this.changeRequestRightsManager.isAuthorizedToEdit(currentUser, changeRequest);
    }

    /**
     * Check if the current user can comment a change request.
     * @param changeRequest the change request for which to check if it can be commented.
     * @return {@code true} if the current user is authorized to comment the change request.
     * @throws ChangeRequestException in case of problem to check the authorization.
     * @see ChangeRequestRightsManager#isAuthorizedToComment(UserReference, ChangeRequest)
     * @since 0.11
     */
    public boolean isAuthorizedToComment(ChangeRequest changeRequest) throws ChangeRequestException
    {
        UserReference currentUser = this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE);
        return this.changeRequestRightsManager.isAuthorizedToComment(currentUser, changeRequest);
    }

    /**
     * Check if the current user is authorized to merge the given changed request.
     *
     * @param changeRequest the change request to be checked for merging authorization.
     * @return {@code true} if the current user has proper rights to merge the given change request.
     * @throws ChangeRequestException in case of problem when checking the role of the user.
     * @since 0.3
     */
    @Unstable
    public boolean isAuthorizedToMerge(ChangeRequest changeRequest) throws ChangeRequestException
    {
        UserReference currentUser = this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE);
        return this.changeRequestRightsManager.isAuthorizedToMerge(currentUser, changeRequest);
    }

    /**
     * Check if the current user can edit the change request.
     *
     * @param changeRequest the change request for which to check the authors.
     * @return {@code true} if the current user is one of the author of the given change request and the change request
     *          is not merged yet.
     * @since 0.9
     */
    public boolean isAuthorizedToOpen(ChangeRequest changeRequest)
    {
        UserReference currentUser = this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE);
        return this.changeRequestRightsManager.isAuthorizedToOpen(currentUser, changeRequest);
    }

    /**
     * Check if the current user is authorized to review the given change request.
     *
     * @param changeRequest the change request about to be reviewed.
     * @return {@code true} if the change request can be reviewed by current user.
     * @throws ChangeRequestException in case of problem when checking if an user is an approver.
     */
    public boolean isAuthorizedToReview(ChangeRequest changeRequest) throws ChangeRequestException
    {
        UserReference currentUserReference = this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE);
        return this.changeRequestRightsManager.isAuthorizedToReview(currentUserReference, changeRequest);
    }

    /**
     * Check if the current user is authorized to perform a review on behalf of the original approver, on the given
     * change request.
     * This should only returns {@code true} if the delegate approver mechanism is enabled, the original approver is an
     * explicit approver and the current user is a delegate approver of them.
     *
     * @param changeRequest the change request for which to check the authorization
     * @param originalApprover the user on behalf of whom the authorization might be given
     * @return {@code true} if the delegate mechanism is enabled and the current user  is not an author, and is a
     *         delegate of the original approver who is also an approver of the given change request.
     * @throws ChangeRequestException in case of problem to resolve the delegate approvers.
     * @since 0.13
     */
    @Unstable
    public boolean isAuthorizedToReviewOnBehalf(ChangeRequest changeRequest, UserReference originalApprover)
        throws ChangeRequestException
    {
        UserReference currentUserReference = this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE);
        return this.changeRequestRightsManager
            .isAuthorizedToReviewOnBehalf(currentUserReference, changeRequest, originalApprover);
    }

    /**
     * Check if the current user is authorized to perform a review as a delegate of one of the approver of the given
     * change request.
     * This should only returns {@code true} if the delegate mechanism is enabled, the change request has explicit
     * approvers, and  the current user is a delegate of at least one of them. Moreover, this cannot return {@code true}
     * if the given user is an author of the change request.
     *
     * @param changeRequest the change request for which to check the authorization
     * @return {@code true} if the delegate mechanism is enabled, the change request has explicit
     *         approvers, the current user is not an author of the change request, and is a delegate of at least one of
     *         the approvers.
     * @throws ChangeRequestException in case of problem to resolve the delegate approvers.
     * @since 0.13
     */
    public boolean isAuthorizedToReviewAsDelegate(ChangeRequest changeRequest)
        throws ChangeRequestException
    {
        UserReference currentUserReference = this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE);
        return this.changeRequestRightsManager.isAuthorizedToReviewAsDelegate(currentUserReference, changeRequest);
    }

    /**
     * Check if the given user is authorized to perform a review as a delegate of one of the approver of the given
     * change request.
     * This should only returns {@code true} if the delegate mechanism is enabled, the change request has explicit
     * approvers, and  the current user is a delegate of at least one of them. Moreover, this cannot return {@code true}
     * if the given user is an author of the change request.
     *
     * @param changeRequest the change request for which to check the authorization
     * @param userReference the user for which to check if they can review
     * @return {@code true} if the delegate mechanism is enabled, the change request has explicit
     *         approvers, the user is not an author of the change request, and is a delegate of at least one of
     *         the approvers.
     * @throws ChangeRequestException in case of problem to resolve the delegate approvers.
     * @since 0.14
     */
    public boolean isAuthorizedToReviewAsDelegate(ChangeRequest changeRequest, UserReference userReference)
        throws ChangeRequestException
    {
        return this.changeRequestRightsManager.isAuthorizedToReviewAsDelegate(userReference, changeRequest);
    }

    /**
     * Check if the given user is authorized to review the given change request.
     *
     * @param changeRequest the change request about to be reviewed.
     * @param userReference the user for which to check if they can review
     * @return {@code true} if the change request can be reviewed by the given user.
     * @throws ChangeRequestException in case of problem when checking if an user is an approver.
     * @since 0.9
     */
    @Unstable
    public boolean isAuthorizedToReview(ChangeRequest changeRequest, UserReference userReference)
        throws ChangeRequestException
    {
        return this.changeRequestRightsManager.isAuthorizedToReview(userReference, changeRequest);
    }

    /**
     * Check if the list of users have view rights to the concerned document: this is a necessary condition for users
     * to be approvers.
     *
     * @param concernedDoc the doc for which to assign approvers
     * @param userReferenceList the proposed approvers for which to test rights
     * @return {@code true} if all proposed users have view rights to the concerned doc
     * @since 1.1
     */
    @Unstable
    public boolean haveApproversViewRights(DocumentReference concernedDoc, List<DocumentReference> userReferenceList)
    {
        boolean result = true;
        if (userReferenceList != null) {
            for (DocumentReference userDoc : userReferenceList) {
                result &= this.authorizationManager.hasAccess(Right.VIEW, userDoc, concernedDoc);
            }
        }
        return result;
    }

    /**
     * Check if the current user is authorized to split the change request.
     *
     * @param changeRequest the change request to split
     * @return {@code true} if the current user is authorized to perform a split
     */
    public boolean isAuthorizedToSplit(ChangeRequest changeRequest)
    {
        UserReference currentUserReference = this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE);
        return this.changeRequestRightsManager.isAuthorizedToSplit(currentUserReference, changeRequest);
    }

    /**
     * Check if the current user is allowed to use change request to edit the given document reference.
     * @param documentReference the reference for which to check authorization
     * @return {@code true} if current user is allowed to perform the edition with change request
     * @throws ChangeRequestException in case of problem when performing the checks
     * @since 1.10
     */
    public boolean isEditWithChangeRequestAllowed(DocumentReference documentReference) throws ChangeRequestException
    {
        UserReference currentUserReference = this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE);
        return this.changeRequestRightsManager.isEditWithChangeRequestAllowed(currentUserReference, documentReference);
    }

    /**
     * Check if the current user is allowed to use change request to create a document having the given reference for
     * parent.
     * @param parentSpaceReference the parent space where the new document would be created
     * @return {@code true} if current user is allowed to create a document with change request at the given place
     * @throws ChangeRequestException in case of problem when performing the checks
     * @since 1.10
     */
    public boolean isCreateWithChangeRequestAllowed(DocumentReference parentSpaceReference)
        throws ChangeRequestException
    {
        UserReference currentUserReference = this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE);
        return this.changeRequestRightsManager.isCreateWithChangeRequestAllowed(currentUserReference,
            parentSpaceReference);
    }
}
