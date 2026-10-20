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

import org.junit.jupiter.api.Test;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestRightsManager;
import org.xwiki.contrib.changerequest.FileChangeSavingChecker.SavingCheckerResult;
import org.xwiki.contrib.changerequest.internal.UserReferenceConverter;
import org.xwiki.contrib.changerequest.internal.approvers.ProvidedApproversValidator;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ChangeRequestAuthorizationScriptService}.
 *
 * @version $Id$
 * @since 0.11
 */
@ComponentTest
class ChangeRequestAuthorizationScriptServiceTest
{
    @InjectMockComponents
    private ChangeRequestAuthorizationScriptService scriptService;

    @MockComponent
    private ChangeRequestRightsManager changeRequestRightsManager;

    @MockComponent
    private AuthorizationManager authorizationManager;

    @MockComponent
    private UserReferenceResolver<CurrentUserReference> currentUserReferenceResolver;

    @MockComponent
    private ProvidedApproversValidator providedApproversValidator;

    @MockComponent
    private UserReferenceConverter userReferenceConverter;

    @Test
    void isAuthorizedToMerge() throws ChangeRequestException
    {
        UserReference userReference = mock(UserReference.class);
        when(this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE)).thenReturn(userReference);
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(this.changeRequestRightsManager.isAuthorizedToMerge(userReference, changeRequest)).thenReturn(true);
        assertTrue(this.scriptService.isAuthorizedToMerge(changeRequest));
        verify(this.changeRequestRightsManager).isAuthorizedToMerge(userReference, changeRequest);
    }

    @Test
    void isAuthorizedToEdit()
    {
        UserReference userReference = mock(UserReference.class);
        when(this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE)).thenReturn(userReference);
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(this.changeRequestRightsManager.isAuthorizedToEdit(userReference, changeRequest))
            .thenReturn(false);
        assertFalse(this.scriptService.isAuthorizedToEdit(changeRequest));

        verify(this.changeRequestRightsManager).isAuthorizedToEdit(userReference, changeRequest);
    }

    @Test
    void isAuthorizedToReview() throws ChangeRequestException
    {
        UserReference userReference = mock(UserReference.class);
        when(this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE)).thenReturn(userReference);
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(this.changeRequestRightsManager.isAuthorizedToReview(userReference, changeRequest)).thenReturn(true);
        assertTrue(this.scriptService.isAuthorizedToReview(changeRequest));
        verify(this.changeRequestRightsManager).isAuthorizedToReview(userReference, changeRequest);
    }

    @Test
    void haveApproversViewRights()
    {
        DocumentReference changedDoc = mock(DocumentReference.class, "changedDoc");
        DocumentReference userDoc1 = mock(DocumentReference.class, "userDoc1");
        DocumentReference userDoc2 = mock(DocumentReference.class, "userDoc2");
        DocumentReference userDoc3 = mock(DocumentReference.class, "userDoc3");

        when(this.authorizationManager.hasAccess(Right.VIEW, userDoc1, changedDoc)).thenReturn(true);
        when(this.authorizationManager.hasAccess(Right.VIEW, userDoc2, changedDoc)).thenReturn(false);
        when(this.authorizationManager.hasAccess(Right.VIEW, userDoc3, changedDoc)).thenReturn(true);
        assertFalse(this.scriptService.haveApproversViewRights(changedDoc, List.of(userDoc1, userDoc2, userDoc3)));

        when(this.authorizationManager.hasAccess(Right.VIEW, userDoc2, changedDoc)).thenReturn(true);
        assertTrue(this.scriptService.haveApproversViewRights(changedDoc, List.of(userDoc1, userDoc2, userDoc3)));
    }

    @Test
    void checkProvidedApprovers() throws ChangeRequestException
    {
        UserReference userReference = mock(UserReference.class);
        DocumentReference userDocReference = mock(DocumentReference.class, "currentUser");
        when(this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE)).thenReturn(userReference);
        when(this.userReferenceConverter.convert(userReference)).thenReturn(userDocReference);

        DocumentReference concernedDoc = mock(DocumentReference.class, "concernedDoc");
        List<DocumentReference> usersApprovers = List.of(mock(DocumentReference.class, "approver"));
        List<DocumentReference> groupsApprovers = List.of(mock(DocumentReference.class, "group"));
        SavingCheckerResult result = new SavingCheckerResult("reason");
        when(this.providedApproversValidator.validate(concernedDoc, userDocReference, usersApprovers,
            groupsApprovers)).thenReturn(result);
        assertSame(result, this.scriptService.checkProvidedApprovers(concernedDoc, usersApprovers, groupsApprovers));

        SavingCheckerResult emptyResult = new SavingCheckerResult();
        when(this.providedApproversValidator.validate(concernedDoc, userDocReference, List.of(), List.of()))
            .thenReturn(emptyResult);
        assertSame(emptyResult, this.scriptService.checkProvidedApprovers(concernedDoc, null, null));
    }
}
