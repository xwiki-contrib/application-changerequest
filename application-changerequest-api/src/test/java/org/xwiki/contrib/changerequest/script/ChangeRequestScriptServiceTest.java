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
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.changerequest.ApproversManager;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestManager;
import org.xwiki.contrib.changerequest.ChangeRequestReference;
import org.xwiki.contrib.changerequest.ChangeRequestStatus;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.FileChangeCompatibilityChecker;
import org.xwiki.contrib.changerequest.MergeApprovalStrategy;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.resource.SerializeResourceReferenceException;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.url.ExtendedURL;
import org.xwiki.user.UserReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
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
    private ResourceReferenceSerializer<ChangeRequestReference, ExtendedURL> urlResourceReferenceSerializer;

    @MockComponent
    private DocumentReferenceResolver<ChangeRequest> documentReferenceResolver;

    @MockComponent
    private ApproversManager<ChangeRequest> changeRequestApproversManager;

    @BeforeComponent
    void setup(MockitoComponentManager componentManager) throws Exception
    {
        componentManager.registerComponent(ComponentManager.class, "context", componentManager);
    }

    @Test
    void getChangeRequest() throws ChangeRequestException
    {
        String id = "someId";
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(this.changeRequestStorageManager.load(id)).thenReturn(Optional.of(changeRequest));
        assertEquals(Optional.of(changeRequest), this.scriptService.getChangeRequest(id));
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

    @Test
    void filterCompatibleChangeRequest(MockitoComponentManager componentManager)
        throws Exception
    {
        DocumentReference crRef1 = new DocumentReference("xwiki", "CR1", "Webhome");
        DocumentReference crRef2 = new DocumentReference("xwiki", Arrays.asList("Foo", "CR2"), "Webhome");
        DocumentReference crRef3 = new DocumentReference("xwiki", "CR3", "Webhome");
        DocumentReference notCRRef = new DocumentReference("xwiki", "NotCR", "Webhome");
        DocumentReference crRef4 = new DocumentReference("xwiki", "CR4", "Webhome");

        ChangeRequest cr1 = mock(ChangeRequest.class);
        ChangeRequest cr2 = mock(ChangeRequest.class);
        ChangeRequest cr3 = mock(ChangeRequest.class);
        ChangeRequest cr4 = mock(ChangeRequest.class);

        DocumentReference newChangeRef = mock(DocumentReference.class);

        when(this.changeRequestStorageManager.load("CR1")).thenReturn(Optional.of(cr1));
        when(this.changeRequestStorageManager.load("CR2")).thenReturn(Optional.of(cr2));
        when(this.changeRequestStorageManager.load("CR3")).thenReturn(Optional.of(cr3));
        when(this.changeRequestStorageManager.load("NotCR")).thenReturn(Optional.empty());
        when(this.changeRequestStorageManager.load("CR4")).thenReturn(Optional.of(cr4));

        FileChangeCompatibilityChecker checker1 =
            componentManager.registerMockComponent(FileChangeCompatibilityChecker.class, "checker1");
        FileChangeCompatibilityChecker checker2 =
            componentManager.registerMockComponent(FileChangeCompatibilityChecker.class, "checker2");

        when(checker1.canChangeOnDocumentBeAdded(cr1, newChangeRef)).thenReturn(true);
        when(checker1.canChangeOnDocumentBeAdded(cr2, newChangeRef)).thenReturn(true);
        when(checker1.canChangeOnDocumentBeAdded(cr3, newChangeRef)).thenReturn(false);
        when(checker1.canChangeOnDocumentBeAdded(cr4, newChangeRef)).thenReturn(true);

        when(checker2.canChangeOnDocumentBeAdded(cr1, newChangeRef)).thenReturn(false);
        when(checker2.canChangeOnDocumentBeAdded(cr2, newChangeRef)).thenReturn(true);
        when(checker2.canChangeOnDocumentBeAdded(cr3, newChangeRef)).thenReturn(true);
        when(checker2.canChangeOnDocumentBeAdded(cr4, newChangeRef)).thenReturn(true);

        List<DocumentReference> expected = Arrays.asList(crRef2, crRef4);

        assertEquals(expected, this.scriptService.filterCompatibleChangeRequest(
            Arrays.asList(crRef1, crRef2, crRef3, notCRRef, crRef4), newChangeRef));
        verify(this.changeRequestStorageManager).load("NotCR");
    }
}
