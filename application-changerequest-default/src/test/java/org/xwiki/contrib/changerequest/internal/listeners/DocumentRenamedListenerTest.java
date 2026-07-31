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
package org.xwiki.contrib.changerequest.internal.listeners;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestStatus;
import org.xwiki.contrib.changerequest.discussions.ChangeRequestDiscussionService;
import org.xwiki.contrib.changerequest.events.ChangeRequestRefactoredEvent;
import org.xwiki.contrib.changerequest.events.ChangeRequestRefactoringEvent;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.refactoring.event.DocumentRenamedEvent;
import org.xwiki.refactoring.job.MoveRequest;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DocumentRenamedListener}.
 *
 * @version $Id$
 * @since 1.23
 */
@ComponentTest
class DocumentRenamedListenerTest
{
    @InjectMockComponents
    private DocumentRenamedListener listener;

    @MockComponent
    private JobProgressManager progressManager;

    @MockComponent
    private ChangeRequestStorageManager storageManager;

    @MockComponent
    private ObservationManager observationManager;

    @MockComponent
    private ChangeRequestDiscussionService changeRequestDiscussionService;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.INFO);

    @Test
    void processLocalEventWhenUpdateLinksDisabled() throws ChangeRequestException
    {
        MoveRequest moveRequest = mock(MoveRequest.class);
        when(moveRequest.isUpdateLinks()).thenReturn(false);

        this.listener.processLocalEvent(new DocumentRenamedEvent(), null, moveRequest);

        verify(this.storageManager, never()).findChangeRequestTargeting(any(DocumentReference.class));
        verify(this.observationManager, never()).notify(any(), any());
    }

    @Test
    void processLocalEventRefactorsMatchingChangeRequests() throws ChangeRequestException
    {
        MoveRequest moveRequest = mock(MoveRequest.class);
        when(moveRequest.isUpdateLinks()).thenReturn(true);
        when(moveRequest.isDeep()).thenReturn(true);

        DocumentReference source = new DocumentReference("wiki", "Space", "Source");
        DocumentReference target = new DocumentReference("wiki", "Space", "Target");
        DocumentRenamedEvent event = new DocumentRenamedEvent(source, target);

        ChangeRequest mergedChangeRequest = mock(ChangeRequest.class);
        when(mergedChangeRequest.getStatus()).thenReturn(ChangeRequestStatus.MERGED);
        when(mergedChangeRequest.getId()).thenReturn("mergedCR");

        ChangeRequest openChangeRequest = mock(ChangeRequest.class);
        when(openChangeRequest.getStatus()).thenReturn(ChangeRequestStatus.READY_FOR_MERGING);
        when(openChangeRequest.getId()).thenReturn("openCR");

        when(this.storageManager.findChangeRequestTargeting(source)).thenReturn(Arrays.asList(
            mergedChangeRequest,
            openChangeRequest
        ));

        this.listener.processLocalEvent(event, null, moveRequest);

        verify(this.storageManager, never()).refactorTargetEntity(mergedChangeRequest, source, target, true);
        verify(this.changeRequestDiscussionService, never())
            .refactorDiscussionFileReference("mergedCR", source, target, true);
        verify(this.observationManager, never())
            .notify(any(ChangeRequestRefactoringEvent.class), eq("mergedCR"));

        verify(this.storageManager).refactorTargetEntity(openChangeRequest, source, target, true);
        verify(this.changeRequestDiscussionService)
            .refactorDiscussionFileReference("openCR", source, target, true);
        verify(this.observationManager).notify(any(ChangeRequestRefactoringEvent.class), eq("openCR"));
        verify(this.observationManager).notify(any(ChangeRequestRefactoredEvent.class), eq("openCR"));

        verify(this.progressManager).pushLevelProgress(1, this.listener);
        verify(this.progressManager).startStep(this.listener);
        verify(this.progressManager).endStep(this.listener);
        verify(this.progressManager).popLevelProgress(this.listener);

        assertEquals("Updating the change requests to refactor document [wiki:Space.Source] to "
            + "[wiki:Space.Target].", this.logCapture.getMessage(0));
        assertEquals("Updating change request [openCR].", this.logCapture.getMessage(1));
    }

    @Test
    void processLocalEventWhenStorageManagerFails() throws ChangeRequestException
    {
        MoveRequest moveRequest = mock(MoveRequest.class);
        when(moveRequest.isUpdateLinks()).thenReturn(true);
        when(moveRequest.isDeep()).thenReturn(false);

        DocumentReference source = new DocumentReference("wiki", "Space", "Source");
        DocumentReference target = new DocumentReference("wiki", "Space", "Target");
        DocumentRenamedEvent event = new DocumentRenamedEvent(source, target);

        when(this.storageManager.findChangeRequestTargeting(source))
            .thenThrow(new ChangeRequestException("error"));

        this.listener.processLocalEvent(event, null, moveRequest);

        verify(this.progressManager).pushLevelProgress(0, this.listener);
        verify(this.progressManager).popLevelProgress(this.listener);
        verify(this.observationManager, never()).notify(any(), any());

        assertEquals("Updating the change requests to refactor document [wiki:Space.Source] to "
            + "[wiki:Space.Target].", this.logCapture.getMessage(0));
        assertEquals("Failed to find change requests using document [wiki:Space.Source].",
            this.logCapture.getMessage(1));
    }

    @Test
    void processLocalEventWhenRefactorTargetEntityFails() throws ChangeRequestException
    {
        MoveRequest moveRequest = mock(MoveRequest.class);
        when(moveRequest.isUpdateLinks()).thenReturn(true);
        when(moveRequest.isDeep()).thenReturn(false);

        DocumentReference source = new DocumentReference("wiki", "Space", "Source");
        DocumentReference target = new DocumentReference("wiki", "Space", "Target");
        DocumentRenamedEvent event = new DocumentRenamedEvent(source, target);

        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(changeRequest.getStatus()).thenReturn(ChangeRequestStatus.READY_FOR_REVIEW);
        when(changeRequest.getId()).thenReturn("crId");

        when(this.storageManager.findChangeRequestTargeting(source)).thenReturn(Collections.singletonList(
            changeRequest));
        doThrow(new ChangeRequestException("error")).when(this.storageManager)
            .refactorTargetEntity(changeRequest, source, target, false);

        this.listener.processLocalEvent(event, null, moveRequest);

        verify(this.observationManager).notify(any(ChangeRequestRefactoringEvent.class), eq("crId"));
        verify(this.observationManager, never()).notify(any(ChangeRequestRefactoredEvent.class), eq("crId"));
        verify(this.changeRequestDiscussionService, never())
            .refactorDiscussionFileReference(eq("crId"), any(), any(), anyBoolean());
        verify(this.progressManager).endStep(this.listener);

        assertEquals("Updating the change requests to refactor document [wiki:Space.Source] to "
            + "[wiki:Space.Target].", this.logCapture.getMessage(0));
        assertEquals("Updating change request [crId].", this.logCapture.getMessage(1));
        assertEquals("Error while refactoring change request [crId] to move document [wiki:Space.Source].",
            this.logCapture.getMessage(2));
    }
}
