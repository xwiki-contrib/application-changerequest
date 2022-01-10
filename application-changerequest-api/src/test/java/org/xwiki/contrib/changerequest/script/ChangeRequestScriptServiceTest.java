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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.contrib.changerequest.ApproversManager;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestManager;
import org.xwiki.contrib.changerequest.ChangeRequestMergeDocumentResult;
import org.xwiki.contrib.changerequest.ChangeRequestReference;
import org.xwiki.contrib.changerequest.ChangeRequestStatus;
import org.xwiki.contrib.changerequest.ConflictResolutionChoice;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.MergeApprovalStrategy;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.diff.Chunk;
import org.xwiki.diff.Conflict;
import org.xwiki.diff.ConflictDecision;
import org.xwiki.diff.Delta;
import org.xwiki.diff.internal.DefaultConflictDecision;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.resource.SerializeResourceReferenceException;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.store.merge.MergeDocumentResult;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.url.ExtendedURL;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ChangeRequestScriptService}.
 *
 * @version $Id$
 * @since 0.5
 */
@ComponentTest
class ChangeRequestScriptServiceTest
{
    @InjectMockComponents
    private ChangeRequestScriptService scriptService;

    @MockComponent
    private ChangeRequestManager changeRequestManager;

    @MockComponent
    private ChangeRequestStorageManager changeRequestStorageManager;

    @MockComponent
    private UserReferenceResolver<CurrentUserReference> currentUserReferenceResolver;

    @MockComponent
    private ResourceReferenceSerializer<ChangeRequestReference, ExtendedURL> urlResourceReferenceSerializer;

    @MockComponent
    private DocumentReferenceResolver<ChangeRequest> documentReferenceResolver;

    @MockComponent
    private ApproversManager<ChangeRequest> changeRequestApproversManager;

    @Test
    void getChangeRequest() throws ChangeRequestException
    {
        String id = "someId";
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(this.changeRequestStorageManager.load(id)).thenReturn(Optional.of(changeRequest));
        assertEquals(Optional.of(changeRequest), this.scriptService.getChangeRequest(id));
    }

    @Test
    void isAuthorizedToMerge() throws ChangeRequestException
    {
        UserReference userReference = mock(UserReference.class);
        when(this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE)).thenReturn(userReference);
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(this.changeRequestManager.isAuthorizedToMerge(userReference, changeRequest)).thenReturn(true);
        assertTrue(this.scriptService.isAuthorizedToMerge(changeRequest));
        verify(this.changeRequestManager).isAuthorizedToMerge(userReference, changeRequest);
    }

    @Test
    void canBeMerged() throws ChangeRequestException
    {
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(this.changeRequestManager.canBeMerged(changeRequest)).thenReturn(true);
        assertTrue(this.scriptService.canBeMerged(changeRequest));
        verify(this.changeRequestManager).canBeMerged(changeRequest);
    }

    @Test
    void getModifiedDocument()
    {
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        DocumentReference documentReference = mock(DocumentReference.class);
        when(changeRequest.getLatestFileChangeFor(documentReference)).thenReturn(Optional.empty());
        assertEquals(Optional.empty(), this.scriptService.getModifiedDocument(changeRequest, documentReference));

        FileChange fileChange = mock(FileChange.class);
        when(changeRequest.getLatestFileChangeFor(documentReference)).thenReturn(Optional.of(fileChange));
        DocumentModelBridge documentModelBridge = mock(DocumentModelBridge.class);
        when(fileChange.getModifiedDocument()).thenReturn(documentModelBridge);

        assertEquals(Optional.of(documentModelBridge),
            this.scriptService.getModifiedDocument(changeRequest, documentReference));
    }

    @Test
    void getChangeRequestWithChangesFor() throws ChangeRequestException
    {
        DocumentReference documentReference = mock(DocumentReference.class);
        List<ChangeRequest> expected = mock(List.class);
        when(this.changeRequestStorageManager.findChangeRequestTargeting(documentReference)).thenReturn(expected);
        assertEquals(expected, this.scriptService.getChangeRequestWithChangesFor(documentReference));
    }

    @Test
    void findChangeRequestMatchingTitle() throws ChangeRequestException
    {
        String title = "someTitle";
        List<DocumentReference> expected = mock(List.class);
        when(this.changeRequestStorageManager.getChangeRequestMatchingName(title)).thenReturn(expected);
        assertEquals(expected, this.scriptService.findChangeRequestMatchingTitle(title));
    }

    @Test
    void getChangeRequestURL() throws SerializeResourceReferenceException, UnsupportedResourceReferenceException
    {
        String action = "merge";
        String id = "someId";
        ChangeRequestReference expectedRef =
            new ChangeRequestReference(ChangeRequestReference.ChangeRequestAction.MERGE, id);
        ExtendedURL extendedURL = mock(ExtendedURL.class);
        when(this.urlResourceReferenceSerializer.serialize(expectedRef)).thenReturn(extendedURL);
        String expectedUrl = "serializedUrl";
        when(extendedURL.serialize()).thenReturn(expectedUrl);
        assertEquals(expectedUrl, this.scriptService.getChangeRequestURL(action, id));
    }

    @Test
    void getChangeRequestDocumentReference()
    {
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        DocumentReference expected = mock(DocumentReference.class);
        when(this.documentReferenceResolver.resolve(changeRequest)).thenReturn(expected);
        assertEquals(expected, this.scriptService.getChangeRequestDocumentReference(changeRequest));
    }

    @Test
    void isAuthorizedToEdit()
    {
        UserReference userReference = mock(UserReference.class);
        when(this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE)).thenReturn(userReference);
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(this.changeRequestManager.isAuthorizedToEdit(userReference, changeRequest))
            .thenReturn(false);
        assertFalse(this.scriptService.isAuthorizedToEdit(changeRequest));

        verify(this.changeRequestManager).isAuthorizedToEdit(userReference, changeRequest);
    }

    @Test
    void setReadyForReview() throws ChangeRequestException
    {
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        this.scriptService.setReadyForReview(changeRequest);

        verify(this.changeRequestManager).updateStatus(changeRequest, ChangeRequestStatus.READY_FOR_REVIEW);
    }

    @Test
    void setDraft() throws ChangeRequestException
    {
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        this.scriptService.setDraft(changeRequest);
        verify(this.changeRequestManager).updateStatus(changeRequest, ChangeRequestStatus.DRAFT);
    }

    @Test
    void setClose() throws ChangeRequestException
    {
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        this.scriptService.setClose(changeRequest);
        verify(this.changeRequestManager).updateStatus(changeRequest, ChangeRequestStatus.CLOSED);
    }

    @Test
    void getMergeDocumentResult() throws ChangeRequestException
    {
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        DocumentReference documentReference = mock(DocumentReference.class);

        when(changeRequest.getLatestFileChangeFor(documentReference)).thenReturn(Optional.empty());
        assertEquals(Optional.empty(), this.scriptService.getMergeDocumentResult(changeRequest, documentReference));

        FileChange fileChange = mock(FileChange.class);
        when(changeRequest.getLatestFileChangeFor(documentReference)).thenReturn(Optional.of(fileChange));
        ChangeRequestMergeDocumentResult expected = mock(ChangeRequestMergeDocumentResult.class);
        when(this.changeRequestManager.getMergeDocumentResult(fileChange)).thenReturn(expected);
        assertEquals(Optional.of(expected),
            this.scriptService.getMergeDocumentResult(changeRequest, documentReference));
    }

    @Test
    void createConflictDecision()
    {
        MergeDocumentResult mergeDocumentResult = mock(MergeDocumentResult.class);
        String conflictReference = "a reference";
        ConflictDecision.DecisionType decisionType = ConflictDecision.DecisionType.CURRENT;
        List<Object> customResolution = mock(List.class);

        Conflict<Object> expectedConflict = mock(Conflict.class);
        when(mergeDocumentResult.getConflicts()).thenReturn(Arrays.asList(
            mock(Conflict.class),
            mock(Conflict.class),
            mock(Conflict.class)
        ));
        assertEquals(Optional.empty(),
            this.scriptService.createConflictDecision(mergeDocumentResult, conflictReference, null, null));

        when(mergeDocumentResult.getConflicts()).thenReturn(Arrays.asList(
            mock(Conflict.class),
            mock(Conflict.class),
            expectedConflict,
            mock(Conflict.class)
        ));
        when(expectedConflict.getReference()).thenReturn(conflictReference);
        ConflictDecision<Object> expected = new DefaultConflictDecision<>(expectedConflict);
        Delta<Object> deltaCurrent = mock(Delta.class);
        when(expectedConflict.getDeltaCurrent()).thenReturn(deltaCurrent);
        Chunk<Object> currentChunk = mock(Chunk.class);
        when(deltaCurrent.getNext()).thenReturn(currentChunk);
        expected.setType(decisionType);
        assertEquals(Optional.of(expected),
            this.scriptService.createConflictDecision(mergeDocumentResult, conflictReference, decisionType, null));

        expected.setCustom(customResolution);
        assertEquals(Optional.of(expected),
            this.scriptService.createConflictDecision(mergeDocumentResult, conflictReference, decisionType,
                customResolution));
    }

    @Test
    void canFixConflict()
    {
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        DocumentReference documentReference = mock(DocumentReference.class);
        when(changeRequest.getLatestFileChangeFor(documentReference)).thenReturn(Optional.empty());
        assertFalse(this.scriptService.canFixConflict(changeRequest, documentReference));

        FileChange fileChange = mock(FileChange.class);
        when(changeRequest.getLatestFileChangeFor(documentReference)).thenReturn(Optional.of(fileChange));
        UserReference userReference = mock(UserReference.class);
        when(this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE)).thenReturn(userReference);
        when(this.changeRequestManager.isAuthorizedToFixConflict(userReference, fileChange)).thenReturn(true);
        assertTrue(this.scriptService.canFixConflict(changeRequest, documentReference));
        verify(this.changeRequestManager).isAuthorizedToFixConflict(userReference, fileChange);
    }

    @Test
    void fixConflicts() throws ChangeRequestException
    {
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        DocumentReference documentReference = mock(DocumentReference.class);
        FileChange fileChange = mock(FileChange.class);
        UserReference userReference = mock(UserReference.class);
        when(changeRequest.getLatestFileChangeFor(documentReference)).thenReturn(Optional.of(fileChange));
        when(this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE)).thenReturn(userReference);
        when(this.changeRequestManager.isAuthorizedToFixConflict(userReference, fileChange)).thenReturn(false);
        assertFalse(this.scriptService
            .fixConflicts(changeRequest, documentReference, ConflictResolutionChoice.CHANGE_REQUEST_VERSION, null));
        verify(this.changeRequestManager, never()).mergeWithConflictDecision(any(), any(), any());

        when(this.changeRequestManager.isAuthorizedToFixConflict(userReference, fileChange)).thenReturn(true);
        List<ConflictDecision<?>> customDecision = mock(List.class);
        when(this.changeRequestManager
            .mergeWithConflictDecision(fileChange, ConflictResolutionChoice.CHANGE_REQUEST_VERSION, customDecision))
            .thenReturn(true);
        assertTrue(this.scriptService.fixConflicts(changeRequest, documentReference,
            ConflictResolutionChoice.CHANGE_REQUEST_VERSION, customDecision));
    }

    @Test
    void getMergeApprovalStrategy() throws ChangeRequestException
    {
        MergeApprovalStrategy mergeApprovalStrategy = mock(MergeApprovalStrategy.class);
        when(this.changeRequestManager.getMergeApprovalStrategy()).thenReturn(mergeApprovalStrategy);
        assertEquals(mergeApprovalStrategy, this.scriptService.getMergeApprovalStrategy());
    }

    @Test
    void canDeletionBeRequested() throws ChangeRequestException
    {
        DocumentReference documentReference = mock(DocumentReference.class);
        when(this.changeRequestManager.canDeletionBeRequested(documentReference)).thenReturn(true);
        assertTrue(this.scriptService.canDeletionBeRequested(documentReference));
        verify(this.changeRequestManager).canDeletionBeRequested(documentReference);
    }

    @Test
    void getApprovers() throws ChangeRequestException
    {
        Set<UserReference> userReferenceSet = mock(Set.class);
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(this.changeRequestApproversManager.getAllApprovers(changeRequest, true)).thenReturn(userReferenceSet);
        assertEquals(userReferenceSet, this.scriptService.getApprovers(changeRequest));
    }

    @Test
    void getFileChange()
    {
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        FileChange fileChange1 = mock(FileChange.class);
        FileChange fileChange2 = mock(FileChange.class);
        FileChange fileChange3 = mock(FileChange.class);
        FileChange fileChange4 = mock(FileChange.class);
        when(changeRequest.getAllFileChanges()).thenReturn(Arrays.asList(
            fileChange1,
            fileChange2,
            fileChange3,
            fileChange4));
        String fileChangeId = "someId42";
        when(fileChange3.getId()).thenReturn(fileChangeId);
        when(fileChange4.getId()).thenReturn(fileChangeId);
        assertEquals(Optional.of(fileChange3), this.scriptService.getFileChange(changeRequest, fileChangeId));
        assertEquals(Optional.empty(), this.scriptService.getFileChange(changeRequest, "anything"));
    }

    @Test
    void isFileChangeOutdated() throws ChangeRequestException
    {
        FileChange fileChange = mock(FileChange.class);
        when(this.changeRequestManager.isFileChangeOutdated(fileChange)).thenReturn(true);
        assertTrue(this.scriptService.isFileChangeOutdated(fileChange));
    }
}
