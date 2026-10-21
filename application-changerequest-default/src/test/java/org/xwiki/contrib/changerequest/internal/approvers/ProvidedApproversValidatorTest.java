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
package org.xwiki.contrib.changerequest.internal.approvers;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.contrib.changerequest.ApproversManager;
import org.xwiki.contrib.changerequest.ChangeRequestConfiguration;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.FileChangeSavingChecker.SavingCheckerResult;
import org.xwiki.contrib.changerequest.internal.UserReferenceConverter;
import org.xwiki.contrib.changerequest.rights.ChangeRequestApproveRight;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;
import org.xwiki.user.group.GroupException;
import org.xwiki.user.group.GroupManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ProvidedApproversValidator}.
 *
 * @version $Id$
 */
@ComponentTest
class ProvidedApproversValidatorTest
{
    private static final Right APPROVE_RIGHT = ChangeRequestApproveRight.getRight();

    private static final DocumentReference DOCUMENT = new DocumentReference("xwiki", "Sandbox", "WebHome");

    private static final DocumentReference AUTHOR = new DocumentReference("xwiki", "XWiki", "Author");

    private static final DocumentReference EXISTING_APPROVER = new DocumentReference("xwiki", "XWiki", "Existing");

    private static final DocumentReference APPROVER_1 = new DocumentReference("xwiki", "XWiki", "Approver1");

    private static final DocumentReference APPROVER_2 = new DocumentReference("xwiki", "XWiki", "Approver2");

    private static final DocumentReference EXISTING_GROUP = new DocumentReference("xwiki", "XWiki", "ExistingGroup");

    private static final DocumentReference GROUP = new DocumentReference("xwiki", "XWiki", "Group");

    @InjectMockComponents
    private ProvidedApproversValidator validator;

    @MockComponent
    private ChangeRequestConfiguration configuration;

    @MockComponent
    private ApproversManager<DocumentReference> documentReferenceApproversManager;

    @MockComponent
    private AuthorizationManager authorizationManager;

    @MockComponent
    private GroupManager groupManager;

    @MockComponent
    private UserReferenceConverter userReferenceConverter;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    private UserReference existingApprover;

    @BeforeEach
    void setup() throws ChangeRequestException
    {
        this.existingApprover = mock(UserReference.class);
        when(this.userReferenceConverter.convert(this.existingApprover)).thenReturn(EXISTING_APPROVER);
        when(this.documentReferenceApproversManager.getAllApprovers(DOCUMENT, false))
            .thenReturn(Set.of(this.existingApprover));
        when(this.documentReferenceApproversManager.getGroupsApprovers(DOCUMENT)).thenReturn(Set.of(EXISTING_GROUP));
        when(this.configuration.getMinimumApprovers()).thenReturn(2);

        // By default all added approvers are allowed: each test removes the access it's about.
        when(this.authorizationManager.hasAccess(any(Right.class), any(DocumentReference.class),
            any(DocumentReference.class))).thenReturn(true);
        when(this.authorizationManager.hasAccess(Right.ADMIN, AUTHOR, DOCUMENT)).thenReturn(false);
    }

    @Test
    void isProvidedApproversListUsed() throws ChangeRequestException
    {
        assertTrue(this.validator.isProvidedApproversListUsed(DOCUMENT));

        when(this.configuration.getMinimumApprovers()).thenReturn(1);
        assertFalse(this.validator.isProvidedApproversListUsed(DOCUMENT));

        when(this.configuration.getMinimumApprovers()).thenReturn(0);
        assertFalse(this.validator.isProvidedApproversListUsed(DOCUMENT));
    }

    @Test
    void validateWhenListNotUsed() throws Exception
    {
        when(this.configuration.getMinimumApprovers()).thenReturn(1);

        assertEquals(new SavingCheckerResult(),
            this.validator.validate(DOCUMENT, AUTHOR, List.of(AUTHOR), List.of()));
        verifyNoInteractions(this.authorizationManager);
    }

    @Test
    void validateValidApprovers() throws Exception
    {
        when(this.groupManager.getMembers(GROUP, true)).thenReturn(List.of(APPROVER_1));

        assertEquals(new SavingCheckerResult(), this.validator.validate(DOCUMENT, AUTHOR,
            List.of(EXISTING_APPROVER, APPROVER_1, APPROVER_2), List.of(EXISTING_GROUP, GROUP)));
    }

    @Test
    void validateWhenAuthorIsAdmin() throws Exception
    {
        when(this.authorizationManager.hasAccess(Right.ADMIN, AUTHOR, DOCUMENT)).thenReturn(true);
        when(this.authorizationManager.hasAccess(APPROVE_RIGHT, APPROVER_1, DOCUMENT)).thenReturn(false);

        assertEquals(new SavingCheckerResult(),
            this.validator.validate(DOCUMENT, AUTHOR, List.of(AUTHOR, APPROVER_1), List.of(GROUP)));
        verifyNoInteractions(this.groupManager);
        verify(this.authorizationManager, never()).hasAccess(APPROVE_RIGHT, APPROVER_1, DOCUMENT);
    }

    @Test
    void validateWhenAuthorIsAddedApprover() throws Exception
    {
        assertEquals(new SavingCheckerResult(ProvidedApproversValidator.AUTHOR_IS_APPROVER_REASON),
            this.validator.validate(DOCUMENT, AUTHOR, List.of(APPROVER_1, AUTHOR), List.of()));
    }

    @Test
    void validateWhenAuthorIsMemberOfAddedGroup() throws Exception
    {
        when(this.groupManager.getMembers(GROUP, true)).thenReturn(List.of(APPROVER_2, AUTHOR));

        assertEquals(new SavingCheckerResult(ProvidedApproversValidator.AUTHOR_IS_APPROVER_REASON),
            this.validator.validate(DOCUMENT, AUTHOR, List.of(APPROVER_1, APPROVER_2), List.of(GROUP)));
    }

    @Test
    void validateOnlyChecksAddedApprovers() throws Exception
    {
        // The author is an existing approver of the document and the existing group: this was accepted before, so
        // it's not the concern of this validator.
        when(this.userReferenceConverter.convert(this.existingApprover)).thenReturn(AUTHOR);
        when(this.authorizationManager.hasAccess(APPROVE_RIGHT, EXISTING_GROUP, DOCUMENT)).thenReturn(false);

        assertEquals(new SavingCheckerResult(), this.validator.validate(DOCUMENT, AUTHOR,
            List.of(AUTHOR, APPROVER_1), List.of(EXISTING_GROUP)));
        verify(this.groupManager, never()).getMembers(any(), anyBoolean());
    }

    @Test
    void validateWhenGroupMembersCannotBeRetrieved() throws Exception
    {
        when(this.groupManager.getMembers(GROUP, true)).thenThrow(new GroupException("Error"));

        assertEquals(new SavingCheckerResult(),
            this.validator.validate(DOCUMENT, AUTHOR, List.of(APPROVER_1, APPROVER_2), List.of(GROUP)));
        assertEquals("Error while trying to get members of group [xwiki:XWiki.Group]: [GroupException: Error]",
            this.logCapture.getMessage(0));
    }

    @Test
    void validateWhenAddedUserCannotView() throws Exception
    {
        when(this.authorizationManager.hasAccess(Right.VIEW, APPROVER_2, DOCUMENT)).thenReturn(false);

        assertEquals(new SavingCheckerResult(ProvidedApproversValidator.MISSING_VIEW_RIGHT_REASON),
            this.validator.validate(DOCUMENT, AUTHOR, List.of(APPROVER_1, APPROVER_2), List.of()));
    }

    @Test
    void validateWhenAddedUserCannotApprove() throws Exception
    {
        when(this.authorizationManager.hasAccess(APPROVE_RIGHT, APPROVER_1, DOCUMENT)).thenReturn(false);

        assertEquals(new SavingCheckerResult(ProvidedApproversValidator.MISSING_APPROVE_RIGHT_REASON),
            this.validator.validate(DOCUMENT, AUTHOR, List.of(APPROVER_1, APPROVER_2), List.of()));
    }

    @Test
    void validateWhenAddedGroupCannotApprove() throws Exception
    {
        when(this.authorizationManager.hasAccess(APPROVE_RIGHT, GROUP, DOCUMENT)).thenReturn(false);

        assertEquals(new SavingCheckerResult(ProvidedApproversValidator.MISSING_APPROVE_RIGHT_REASON),
            this.validator.validate(DOCUMENT, AUTHOR, List.of(APPROVER_1, APPROVER_2), List.of(GROUP)));
    }
}
