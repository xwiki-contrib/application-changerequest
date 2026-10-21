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

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.contrib.changerequest.ApproversManager;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.FileChangeSavingChecker.SavingCheckerResult;
import org.xwiki.contrib.changerequest.internal.UserReferenceConverter;
import org.xwiki.contrib.changerequest.internal.approvers.ProvidedApproversValidator;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ProvidedApproversSavingChecker}.
 *
 * @version $Id$
 */
@ComponentTest
class ProvidedApproversSavingCheckerTest
{
    private static final SavingCheckerResult FAILURE = new SavingCheckerResult("failure");

    @InjectMockComponents
    private ProvidedApproversSavingChecker checker;

    @MockComponent
    private ProvidedApproversValidator providedApproversValidator;

    @MockComponent
    private ApproversManager<XWikiDocument> documentApproversManager;

    @MockComponent
    private UserReferenceConverter userReferenceConverter;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    private FileChange fileChange;

    private XWikiDocument modifiedDocument;

    private DocumentReference targetReference;

    private DocumentReference authorReference;

    private DocumentReference approverReference;

    private Set<DocumentReference> groupsApprovers;

    @BeforeEach
    void setup() throws ChangeRequestException
    {
        this.fileChange = mock(FileChange.class, "fileChange");
        this.modifiedDocument = mock(XWikiDocument.class);
        this.targetReference = mock(DocumentReference.class, "target");
        when(this.fileChange.getType()).thenReturn(FileChange.FileChangeType.EDITION);
        when(this.fileChange.getModifiedDocument()).thenReturn(this.modifiedDocument);
        when(this.fileChange.getTargetEntity()).thenReturn(this.targetReference);

        UserReference author = mock(UserReference.class, "author");
        this.authorReference = mock(DocumentReference.class, "authorRef");
        when(this.fileChange.getAuthor()).thenReturn(author);
        when(this.userReferenceConverter.convert(author)).thenReturn(this.authorReference);

        UserReference approver = mock(UserReference.class, "approver");
        this.approverReference = mock(DocumentReference.class, "approverRef");
        when(this.userReferenceConverter.convert(approver)).thenReturn(this.approverReference);
        when(this.documentApproversManager.getAllApprovers(this.modifiedDocument, false))
            .thenReturn(Set.of(approver));

        this.groupsApprovers = Set.of(mock(DocumentReference.class, "group"));
        when(this.documentApproversManager.getGroupsApprovers(this.modifiedDocument))
            .thenReturn(this.groupsApprovers);
        when(this.providedApproversValidator.isProvidedApproversListUsed(this.targetReference)).thenReturn(true);
    }

    @Test
    void canChangeOnDocumentBeAddedWithoutFileChange()
    {
        assertEquals(new SavingCheckerResult(), this.checker.canChangeOnDocumentBeAdded(null, null, null));
    }

    @Test
    void canChangeRequestBeCreatedWith() throws ChangeRequestException
    {
        when(this.providedApproversValidator.validate(this.targetReference, this.authorReference,
            List.of(this.approverReference), this.groupsApprovers)).thenReturn(FAILURE);

        assertEquals(FAILURE, this.checker.canChangeRequestBeCreatedWith(this.fileChange));
    }

    @Test
    void canChangeOnDocumentBeAdded() throws ChangeRequestException
    {
        when(this.providedApproversValidator.validate(this.targetReference, this.authorReference,
            List.of(this.approverReference), this.groupsApprovers)).thenReturn(new SavingCheckerResult());

        assertEquals(new SavingCheckerResult(),
            this.checker.canChangeOnDocumentBeAdded(mock(ChangeRequest.class), this.fileChange));
    }

    @Test
    void checkWhenProvidedApproversListNotUsed() throws ChangeRequestException
    {
        when(this.providedApproversValidator.isProvidedApproversListUsed(this.targetReference)).thenReturn(false);

        assertEquals(new SavingCheckerResult(), this.checker.canChangeRequestBeCreatedWith(this.fileChange));
        verifyNoInteractions(this.documentApproversManager);
        verify(this.providedApproversValidator, never()).validate(any(), any(), any(), any());
    }

    @Test
    void checkIgnoresOtherChangeTypes()
    {
        when(this.fileChange.getType()).thenReturn(FileChange.FileChangeType.CREATION);
        assertEquals(new SavingCheckerResult(), this.checker.canChangeRequestBeCreatedWith(this.fileChange));

        when(this.fileChange.getType()).thenReturn(FileChange.FileChangeType.DELETION);
        assertEquals(new SavingCheckerResult(), this.checker.canChangeRequestBeCreatedWith(this.fileChange));

        verifyNoInteractions(this.providedApproversValidator);
    }

    @Test
    void checkWhenApproversCannotBeRetrieved() throws ChangeRequestException
    {
        when(this.documentApproversManager.getGroupsApprovers(this.modifiedDocument))
            .thenThrow(new ChangeRequestException("Error"));

        assertEquals(new SavingCheckerResult("changerequest.checkers.providedApprovers.error"),
            this.checker.canChangeRequestBeCreatedWith(this.fileChange));
        assertEquals("Error while checking the approvers provided in filechange [fileChange]: "
            + "[ChangeRequestException: Error]", this.logCapture.getMessage(0));
    }
}
