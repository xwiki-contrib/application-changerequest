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
package org.xwiki.contrib.changerequest.internal.checkers;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.xwiki.contrib.changerequest.ApproversManager;
import org.xwiki.contrib.changerequest.ChangeRequestConfiguration;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.FileChangeSavingChecker;
import org.xwiki.contrib.changerequest.internal.UserReferenceConverter;
import org.xwiki.contrib.changerequest.rights.ChangeRequestApproveRight;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ApproversRightChecker}.
 *
 * @version $Id$
 */
@ComponentTest
class ApproversRightCheckerTest
{
    private static final String FAILURE_REASON = "changerequest.checkers.approversright.incompatibilityReason";

    private static final Right APPROVE_RIGHT = ChangeRequestApproveRight.getRight();

    @InjectMockComponents
    private ApproversRightChecker approversRightChecker;

    @MockComponent
    private ChangeRequestConfiguration configuration;

    @MockComponent
    private ApproversManager<XWikiDocument> fileChangeApproversManager;

    @MockComponent
    private AuthorizationManager authorizationManager;

    @MockComponent
    private UserReferenceConverter userReferenceConverter;

    @Test
    void canChangeOnDocumentBeAdded()
    {
        assertEquals(new FileChangeSavingChecker.SavingCheckerResult(),
            this.approversRightChecker.canChangeOnDocumentBeAdded(null, null, null));
    }

    @Test
    void canChangeOnDocumentBeAddedWithFileChange() throws ChangeRequestException
    {
        FileChange fileChange = mock(FileChange.class);
        when(this.configuration.acceptOnlyAllowedApprovers()).thenReturn(false);

        assertEquals(new FileChangeSavingChecker.SavingCheckerResult(),
            this.approversRightChecker.canChangeOnDocumentBeAdded(null, fileChange));
        verifyNoInteractions(fileChange);

        when(this.configuration.acceptOnlyAllowedApprovers()).thenReturn(true);
        when(fileChange.getType()).thenReturn(FileChange.FileChangeType.DELETION);
        assertEquals(new FileChangeSavingChecker.SavingCheckerResult(),
            this.approversRightChecker.canChangeOnDocumentBeAdded(null, fileChange));
        verify(fileChange, never()).getModifiedDocument();

        when(fileChange.getType()).thenReturn(FileChange.FileChangeType.EDITION);
        XWikiDocument modifiedDoc = mock(XWikiDocument.class);
        when(fileChange.getModifiedDocument()).thenReturn(modifiedDoc);
        DocumentReference modifiedDocRef = mock(DocumentReference.class, "modifiedDocRef");
        when(modifiedDoc.getDocumentReference()).thenReturn(modifiedDocRef);

        UserReference approver1 = mock(UserReference.class, "approver1");
        UserReference approver2 = mock(UserReference.class, "approver2");
        UserReference approver3 = mock(UserReference.class, "approver3");
        when(this.fileChangeApproversManager.getAllApprovers(modifiedDoc, false))
            .thenReturn(new LinkedHashSet<>(List.of(approver1, approver2, approver3)));

        DocumentReference approverDoc1 = mock(DocumentReference.class, "approverDoc1");
        DocumentReference approverDoc2 = mock(DocumentReference.class, "approverDoc2");
        DocumentReference approverDoc3 = mock(DocumentReference.class, "approverDoc3");

        when(this.userReferenceConverter.convert(approver1)).thenReturn(approverDoc1);
        when(this.userReferenceConverter.convert(approver2)).thenReturn(approverDoc2);
        when(this.userReferenceConverter.convert(approver3)).thenReturn(approverDoc3);

        when(this.authorizationManager.hasAccess(APPROVE_RIGHT, approverDoc1, modifiedDocRef)).thenReturn(true);
        when(this.authorizationManager.hasAccess(APPROVE_RIGHT, approverDoc2, modifiedDocRef)).thenReturn(false);

        assertEquals(new FileChangeSavingChecker.SavingCheckerResult(FAILURE_REASON),
            this.approversRightChecker.canChangeOnDocumentBeAdded(null, fileChange));
        verify(this.authorizationManager, never()).hasAccess(APPROVE_RIGHT, approverDoc3, modifiedDocRef);
        verify(this.fileChangeApproversManager, never()).getGroupsApprovers(modifiedDoc);

        when(fileChange.getType()).thenReturn(FileChange.FileChangeType.CREATION);

        when(this.authorizationManager.hasAccess(APPROVE_RIGHT, approverDoc2, modifiedDocRef)).thenReturn(true);
        when(this.authorizationManager.hasAccess(APPROVE_RIGHT, approverDoc3, modifiedDocRef)).thenReturn(true);

        DocumentReference groupRef1 = mock(DocumentReference.class, "groupRef1");
        DocumentReference groupRef2 = mock(DocumentReference.class, "groupRef2");
        when(this.fileChangeApproversManager.getGroupsApprovers(modifiedDoc)).thenReturn(Set.of(groupRef1, groupRef2));

        when(this.authorizationManager.hasAccess(APPROVE_RIGHT, groupRef1, modifiedDocRef)).thenReturn(true);
        when(this.authorizationManager.hasAccess(APPROVE_RIGHT, groupRef2, modifiedDocRef)).thenReturn(false);
        assertEquals(new FileChangeSavingChecker.SavingCheckerResult(FAILURE_REASON),
            this.approversRightChecker.canChangeOnDocumentBeAdded(null, fileChange));

        when(this.authorizationManager.hasAccess(APPROVE_RIGHT, groupRef2, modifiedDocRef)).thenReturn(true);
        assertEquals(new FileChangeSavingChecker.SavingCheckerResult(),
            this.approversRightChecker.canChangeOnDocumentBeAdded(null, fileChange));
    }

    @Test
    void canChangeRequestBeCreatedWith() throws ChangeRequestException
    {
        FileChange fileChange = mock(FileChange.class);
        when(this.configuration.acceptOnlyAllowedApprovers()).thenReturn(false);

        assertEquals(new FileChangeSavingChecker.SavingCheckerResult(),
            this.approversRightChecker.canChangeRequestBeCreatedWith(fileChange));
        verifyNoInteractions(fileChange);

        when(this.configuration.acceptOnlyAllowedApprovers()).thenReturn(true);
        when(fileChange.getType()).thenReturn(FileChange.FileChangeType.DELETION);
        assertEquals(new FileChangeSavingChecker.SavingCheckerResult(),
            this.approversRightChecker.canChangeRequestBeCreatedWith(fileChange));
        verify(fileChange, never()).getModifiedDocument();

        when(fileChange.getType()).thenReturn(FileChange.FileChangeType.EDITION);
        XWikiDocument modifiedDoc = mock(XWikiDocument.class);
        when(fileChange.getModifiedDocument()).thenReturn(modifiedDoc);
        DocumentReference modifiedDocRef = mock(DocumentReference.class, "modifiedDocRef");
        when(modifiedDoc.getDocumentReference()).thenReturn(modifiedDocRef);

        UserReference approver1 = mock(UserReference.class, "approver1");
        UserReference approver2 = mock(UserReference.class, "approver2");
        UserReference approver3 = mock(UserReference.class, "approver3");
        when(this.fileChangeApproversManager.getAllApprovers(modifiedDoc, false))
            .thenReturn(new LinkedHashSet<>(List.of(approver1, approver2, approver3)));

        DocumentReference approverDoc1 = mock(DocumentReference.class, "approverDoc1");
        DocumentReference approverDoc2 = mock(DocumentReference.class, "approverDoc2");
        DocumentReference approverDoc3 = mock(DocumentReference.class, "approverDoc3");

        when(this.userReferenceConverter.convert(approver1)).thenReturn(approverDoc1);
        when(this.userReferenceConverter.convert(approver2)).thenReturn(approverDoc2);
        when(this.userReferenceConverter.convert(approver3)).thenReturn(approverDoc3);

        when(this.authorizationManager.hasAccess(APPROVE_RIGHT, approverDoc1, modifiedDocRef)).thenReturn(true);
        when(this.authorizationManager.hasAccess(APPROVE_RIGHT, approverDoc2, modifiedDocRef)).thenReturn(false);

        assertEquals(new FileChangeSavingChecker.SavingCheckerResult(FAILURE_REASON),
            this.approversRightChecker.canChangeRequestBeCreatedWith(fileChange));
        verify(this.authorizationManager, never()).hasAccess(APPROVE_RIGHT, approverDoc3, modifiedDocRef);
        verify(this.fileChangeApproversManager, never()).getGroupsApprovers(modifiedDoc);

        when(fileChange.getType()).thenReturn(FileChange.FileChangeType.CREATION);

        when(this.authorizationManager.hasAccess(APPROVE_RIGHT, approverDoc2, modifiedDocRef)).thenReturn(true);
        when(this.authorizationManager.hasAccess(APPROVE_RIGHT, approverDoc3, modifiedDocRef)).thenReturn(true);

        DocumentReference groupRef1 = mock(DocumentReference.class, "groupRef1");
        DocumentReference groupRef2 = mock(DocumentReference.class, "groupRef2");
        when(this.fileChangeApproversManager.getGroupsApprovers(modifiedDoc)).thenReturn(Set.of(groupRef1, groupRef2));

        when(this.authorizationManager.hasAccess(APPROVE_RIGHT, groupRef1, modifiedDocRef)).thenReturn(true);
        when(this.authorizationManager.hasAccess(APPROVE_RIGHT, groupRef2, modifiedDocRef)).thenReturn(false);
        assertEquals(new FileChangeSavingChecker.SavingCheckerResult(FAILURE_REASON),
            this.approversRightChecker.canChangeRequestBeCreatedWith(fileChange));

        when(this.authorizationManager.hasAccess(APPROVE_RIGHT, groupRef2, modifiedDocRef)).thenReturn(true);
        assertEquals(new FileChangeSavingChecker.SavingCheckerResult(),
            this.approversRightChecker.canChangeRequestBeCreatedWith(fileChange));
    }
}