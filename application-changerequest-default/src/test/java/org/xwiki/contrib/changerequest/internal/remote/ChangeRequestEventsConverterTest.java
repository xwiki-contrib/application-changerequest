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
package org.xwiki.contrib.changerequest.internal.remote;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestReview;
import org.xwiki.contrib.changerequest.ChangeRequestStatus;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.events.ApproversUpdatedEvent;
import org.xwiki.contrib.changerequest.events.ChangeRequestCreatedEvent;
import org.xwiki.contrib.changerequest.events.ChangeRequestFileChangeAddedEvent;
import org.xwiki.contrib.changerequest.events.ChangeRequestMergeFailedEvent;
import org.xwiki.contrib.changerequest.events.ChangeRequestMergedEvent;
import org.xwiki.contrib.changerequest.events.ChangeRequestMergingEvent;
import org.xwiki.contrib.changerequest.events.ChangeRequestRebasedEvent;
import org.xwiki.contrib.changerequest.events.ChangeRequestReviewAddedEvent;
import org.xwiki.contrib.changerequest.events.ChangeRequestStatusChangedEvent;
import org.xwiki.contrib.changerequest.events.ChangeRequestUpdatedEvent;
import org.xwiki.contrib.changerequest.events.ChangeRequestUpdatedFileChangeEvent;
import org.xwiki.contrib.changerequest.events.ChangeRequestUpdatingFileChangeEvent;
import org.xwiki.contrib.changerequest.events.FileChangeDocumentSavedEvent;
import org.xwiki.contrib.changerequest.events.FileChangeRebasedEvent;
import org.xwiki.contrib.changerequest.events.SplitBeginChangeRequestEvent;
import org.xwiki.contrib.changerequest.events.SplitEndChangeRequestEvent;
import org.xwiki.contrib.changerequest.events.StaleChangeRequestEvent;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.LocalEventData;
import org.xwiki.observation.remote.RemoteEventData;
import org.xwiki.observation.remote.converter.LocalEventConverter;
import org.xwiki.observation.remote.converter.RemoteEventConverter;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ChangeRequestEventsConverter}.
 *
 * @version $Id$
 * @since 1.5
 */
@ComponentTest
class ChangeRequestEventsConverterTest
{
    @InjectMockComponents(role = LocalEventConverter.class)
    private ChangeRequestEventsConverter converterToRemote;

    @InjectMockComponents(role = RemoteEventConverter.class)
    private ChangeRequestEventsConverter converterFromRemote;

    @MockComponent
    private ChangeRequestEventsConverterHelper helper;

    @MockComponent
    private XWikiDocumentEventConverterSerializer serializer;

    @Test
    void toRemoteChangeRequestCreatedEvent()
    {
        LocalEventData localEvent = mock(LocalEventData.class);
        RemoteEventData remoteEvent = mock(RemoteEventData.class);

        ChangeRequestCreatedEvent event = new ChangeRequestCreatedEvent();
        when(localEvent.getEvent()).thenReturn(event);
        String source = "someCr43";
        when(localEvent.getSource()).thenReturn(source);

        assertTrue(this.converterToRemote.toRemote(localEvent, remoteEvent));
        verify(remoteEvent).setEvent(event);
        verify(remoteEvent).setSource(source);
        verify(remoteEvent, never()).setData(any());
    }

    @Test
    void toRemoteChangeRequestMergedEvent()
    {
        LocalEventData localEvent = mock(LocalEventData.class);
        RemoteEventData remoteEvent = mock(RemoteEventData.class);

        ChangeRequestMergedEvent event = new ChangeRequestMergedEvent();
        when(localEvent.getEvent()).thenReturn(event);
        String source = "someCr4513";
        when(localEvent.getSource()).thenReturn(source);

        assertTrue(this.converterToRemote.toRemote(localEvent, remoteEvent));
        verify(remoteEvent).setEvent(event);
        verify(remoteEvent).setSource(source);
        verify(remoteEvent, never()).setData(any());
    }

    @Test
    void toRemoteChangeRequestMergeFailedEvent()
    {
        LocalEventData localEvent = mock(LocalEventData.class);
        RemoteEventData remoteEvent = mock(RemoteEventData.class);

        ChangeRequestMergeFailedEvent event = new ChangeRequestMergeFailedEvent();
        when(localEvent.getEvent()).thenReturn(event);
        String source = "somfdeCr43";
        when(localEvent.getSource()).thenReturn(source);

        assertTrue(this.converterToRemote.toRemote(localEvent, remoteEvent));
        verify(remoteEvent).setEvent(event);
        verify(remoteEvent).setSource(source);
        verify(remoteEvent, never()).setData(any());
    }

    @Test
    void toRemoteChangeRequestRebasedEvent()
    {
        LocalEventData localEvent = mock(LocalEventData.class);
        RemoteEventData remoteEvent = mock(RemoteEventData.class);

        ChangeRequestRebasedEvent event = new ChangeRequestRebasedEvent();
        when(localEvent.getEvent()).thenReturn(event);
        String source = "someCr43fdffdfd";
        when(localEvent.getSource()).thenReturn(source);

        assertTrue(this.converterToRemote.toRemote(localEvent, remoteEvent));
        verify(remoteEvent).setEvent(event);
        verify(remoteEvent).setSource(source);
        verify(remoteEvent, never()).setData(any());
    }

    @Test
    void toRemoteChangeRequestUpdatedEvent()
    {
        LocalEventData localEvent = mock(LocalEventData.class);
        RemoteEventData remoteEvent = mock(RemoteEventData.class);

        ChangeRequestUpdatedEvent event = new ChangeRequestUpdatedEvent();
        when(localEvent.getEvent()).thenReturn(event);
        String source = "someCr4345";
        when(localEvent.getSource()).thenReturn(source);

        assertTrue(this.converterToRemote.toRemote(localEvent, remoteEvent));
        verify(remoteEvent).setEvent(event);
        verify(remoteEvent).setSource(source);
        verify(remoteEvent, never()).setData(any());
    }

    @Test
    void toRemoteStaleChangeRequestEvent()
    {
        LocalEventData localEvent = mock(LocalEventData.class);
        RemoteEventData remoteEvent = mock(RemoteEventData.class);

        StaleChangeRequestEvent event = new StaleChangeRequestEvent();
        when(localEvent.getEvent()).thenReturn(event);
        String source = "someCr4354";
        when(localEvent.getSource()).thenReturn(source);

        assertTrue(this.converterToRemote.toRemote(localEvent, remoteEvent));
        verify(remoteEvent).setEvent(event);
        verify(remoteEvent).setSource(source);
        verify(remoteEvent, never()).setData(any());
    }

    @Test
    void toRemoteChangeRequestMergingEvent()
    {
        LocalEventData localEvent = mock(LocalEventData.class);
        RemoteEventData remoteEvent = mock(RemoteEventData.class);

        ChangeRequestMergingEvent event = new ChangeRequestMergingEvent();
        when(localEvent.getEvent()).thenReturn(event);
        String source = "someOtherCr4354";
        when(localEvent.getSource()).thenReturn(source);

        assertTrue(this.converterToRemote.toRemote(localEvent, remoteEvent));
        verify(remoteEvent).setEvent(event);
        verify(remoteEvent).setSource(source);
        verify(remoteEvent, never()).setData(any());
    }

    @Test
    void toRemoteChangeRequestUpdatingFileChangeEvent()
    {
        LocalEventData localEvent = mock(LocalEventData.class);
        RemoteEventData remoteEvent = mock(RemoteEventData.class);

        ChangeRequestUpdatingFileChangeEvent event = new ChangeRequestUpdatingFileChangeEvent();
        when(localEvent.getEvent()).thenReturn(event);
        String source = "someCr43544545";
        when(localEvent.getSource()).thenReturn(source);

        assertTrue(this.converterToRemote.toRemote(localEvent, remoteEvent));
        verify(remoteEvent).setEvent(event);
        verify(remoteEvent).setSource(source);
        verify(remoteEvent, never()).setData(any());
    }

    @Test
    void toRemoteSplitBeginChangeRequestEvent()
    {
        LocalEventData localEvent = mock(LocalEventData.class);
        RemoteEventData remoteEvent = mock(RemoteEventData.class);

        SplitBeginChangeRequestEvent event = new SplitBeginChangeRequestEvent();
        when(localEvent.getEvent()).thenReturn(event);
        String source = "someBlaCr4354";
        when(localEvent.getSource()).thenReturn(source);

        assertTrue(this.converterToRemote.toRemote(localEvent, remoteEvent));
        verify(remoteEvent).setEvent(event);
        verify(remoteEvent).setSource(source);
        verify(remoteEvent, never()).setData(any());
    }

    @Test
    void toRemoteChangeRequestFileChangeAddedEvent()
    {
        LocalEventData localEvent = mock(LocalEventData.class);
        RemoteEventData remoteEvent = mock(RemoteEventData.class);

        ChangeRequestFileChangeAddedEvent event = new ChangeRequestFileChangeAddedEvent();
        when(localEvent.getEvent()).thenReturn(event);
        String source = "aSourceCR";
        when(localEvent.getSource()).thenReturn(source);
        FileChange fileChange = mock(FileChange.class);
        when(localEvent.getData()).thenReturn(fileChange);
        String fileChangeId = "someFileChangeId";
        when(fileChange.getId()).thenReturn(fileChangeId);

        assertTrue(this.converterToRemote.toRemote(localEvent, remoteEvent));
        verify(remoteEvent).setEvent(event);
        verify(remoteEvent).setSource(source);
        verify(remoteEvent).setData(fileChangeId);
    }

    @Test
    void toRemoteFileChangeRebasedEvent()
    {
        LocalEventData localEvent = mock(LocalEventData.class);
        RemoteEventData remoteEvent = mock(RemoteEventData.class);

        FileChangeRebasedEvent event = new FileChangeRebasedEvent();
        when(localEvent.getEvent()).thenReturn(event);
        String source = "aSourceCR45";
        when(localEvent.getSource()).thenReturn(source);
        FileChange fileChange = mock(FileChange.class);
        when(localEvent.getData()).thenReturn(fileChange);
        String fileChangeId = "someFileChangeId28";
        when(fileChange.getId()).thenReturn(fileChangeId);

        assertTrue(this.converterToRemote.toRemote(localEvent, remoteEvent));
        verify(remoteEvent).setEvent(event);
        verify(remoteEvent).setSource(source);
        verify(remoteEvent).setData(fileChangeId);
    }

    @Test
    void toRemoteApproversUpdatedEvent()
    {
        LocalEventData localEvent = mock(LocalEventData.class);
        RemoteEventData remoteEvent = mock(RemoteEventData.class);

        ApproversUpdatedEvent event = new ApproversUpdatedEvent();
        when(localEvent.getEvent()).thenReturn(event);
        XWikiDocument source = mock(XWikiDocument.class);
        when(localEvent.getSource()).thenReturn(source);
        Pair<HashSet<String>, HashSet<String>> data = Pair.of(
            new HashSet<>(Collections.singleton("bla")),
            new HashSet<>(Collections.emptySet())
        );
        when(localEvent.getData()).thenReturn(data);

        String serializedDoc = "FOo";
        when(this.serializer.serializeXWikiDocument(source)).thenReturn(serializedDoc);

        assertTrue(this.converterToRemote.toRemote(localEvent, remoteEvent));
        verify(remoteEvent).setEvent(event);
        verify(remoteEvent).setSource(serializedDoc);
        verify(remoteEvent).setData(data);
    }

    @Test
    void toRemoteChangeRequestUpdatedFileChangeEvent()
    {
        LocalEventData localEvent = mock(LocalEventData.class);
        RemoteEventData remoteEvent = mock(RemoteEventData.class);

        ChangeRequestUpdatedFileChangeEvent event = new ChangeRequestUpdatedFileChangeEvent();
        when(localEvent.getEvent()).thenReturn(event);
        String source = "aSourceCR4589";
        when(localEvent.getSource()).thenReturn(source);

        assertTrue(this.converterToRemote.toRemote(localEvent, remoteEvent));
        verify(remoteEvent).setEvent(event);
        verify(remoteEvent).setSource(source);
        verify(remoteEvent, never()).setData(any());

        FileChange fileChange = mock(FileChange.class);
        when(localEvent.getData()).thenReturn(fileChange);
        String fileChangeId = "someFileChangeId2836";
        when(fileChange.getId()).thenReturn(fileChangeId);

        assertTrue(this.converterToRemote.toRemote(localEvent, remoteEvent));
        verify(remoteEvent, times(2)).setEvent(event);
        verify(remoteEvent, times(2)).setSource(source);
        verify(remoteEvent).setData(fileChangeId);
    }

    @Test
    void toRemoteChangeRequestReviewAddedEvent()
    {
        LocalEventData localEvent = mock(LocalEventData.class);
        RemoteEventData remoteEvent = mock(RemoteEventData.class);

        ChangeRequestReviewAddedEvent event = new ChangeRequestReviewAddedEvent();
        when(localEvent.getEvent()).thenReturn(event);
        String source = "aSourceCR4558";
        when(localEvent.getSource()).thenReturn(source);
        ChangeRequestReview review = mock(ChangeRequestReview.class);
        when(localEvent.getData()).thenReturn(review);

        String reviewId = "someReview";
        when(review.getId()).thenReturn(reviewId);

        assertTrue(this.converterToRemote.toRemote(localEvent, remoteEvent));
        verify(remoteEvent).setEvent(event);
        verify(remoteEvent).setSource(source);
        verify(remoteEvent).setData(reviewId);
    }

    @Test
    void toRemoteChangeRequestStatusChangedEvent()
    {
        LocalEventData localEvent = mock(LocalEventData.class);
        RemoteEventData remoteEvent = mock(RemoteEventData.class);

        ChangeRequestStatusChangedEvent event = new ChangeRequestStatusChangedEvent();
        when(localEvent.getEvent()).thenReturn(event);
        String source = "myCR45";
        when(localEvent.getSource()).thenReturn(source);

        ChangeRequestStatus[] data = new ChangeRequestStatus[] {
            ChangeRequestStatus.DRAFT,
            ChangeRequestStatus.CLOSED
        };
        when(localEvent.getData()).thenReturn(data);

        ArrayList<String> expectedData = new ArrayList<>(List.of("DRAFT", "CLOSED"));
        assertTrue(this.converterToRemote.toRemote(localEvent, remoteEvent));
        verify(remoteEvent).setEvent(event);
        verify(remoteEvent).setSource(source);
        verify(remoteEvent).setData(expectedData);
    }

    @Test
    void toRemoteFileChangeDocumentSavedEvent()
    {
        LocalEventData localEvent = mock(LocalEventData.class);
        RemoteEventData remoteEvent = mock(RemoteEventData.class);

        FileChangeDocumentSavedEvent event = new FileChangeDocumentSavedEvent();
        when(localEvent.getEvent()).thenReturn(event);
        FileChange fileChange = mock(FileChange.class);
        when(localEvent.getSource()).thenReturn(fileChange);

        XWikiDocument document = mock(XWikiDocument.class);
        when(localEvent.getData()).thenReturn(document);

        ChangeRequest changeRequest = mock(ChangeRequest.class);
        String changeRequestId = "someCrId";
        when(changeRequest.getId()).thenReturn(changeRequestId);
        String fileChangeId = "fooFileChange";
        when(fileChange.getId()).thenReturn(fileChangeId);
        when(fileChange.getChangeRequest()).thenReturn(changeRequest);

        String serializedDoc = "fooXWikiDocument";
        when(this.serializer.serializeXWikiDocument(document)).thenReturn(serializedDoc);

        assertTrue(this.converterToRemote.toRemote(localEvent, remoteEvent));
        verify(remoteEvent).setEvent(event);
        verify(remoteEvent).setSource(Pair.of(changeRequestId, fileChangeId));
        verify(remoteEvent).setData(serializedDoc);
    }

    @Test
    void toRemoteSplitEndChangeRequestEvent()
    {
        LocalEventData localEvent = mock(LocalEventData.class);
        RemoteEventData remoteEvent = mock(RemoteEventData.class);

        SplitEndChangeRequestEvent event = new SplitEndChangeRequestEvent();
        when(localEvent.getEvent()).thenReturn(event);
        String source = "originalCr";
        when(localEvent.getSource()).thenReturn(source);

        ChangeRequest cr1 = mock(ChangeRequest.class);
        ChangeRequest cr2 = mock(ChangeRequest.class);
        ChangeRequest cr3 = mock(ChangeRequest.class);
        when(localEvent.getData()).thenReturn(List.of(cr1, cr2, cr3));

        String cr1Id = "cr1Id";
        String cr2Id = "cr2Id";
        String cr3Id = "cr3Id";
        when(cr1.getId()).thenReturn(cr1Id);
        when(cr2.getId()).thenReturn(cr2Id);
        when(cr3.getId()).thenReturn(cr3Id);

        assertTrue(this.converterToRemote.toRemote(localEvent, remoteEvent));
        verify(remoteEvent).setEvent(event);
        verify(remoteEvent).setSource(source);
        verify(remoteEvent).setData(new ArrayList<>(List.of(cr1Id, cr2Id, cr3Id)));
    }

    @Test
    void toRemoteAnyEvent()
    {
        LocalEventData localEvent = mock(LocalEventData.class);
        RemoteEventData remoteEvent = mock(RemoteEventData.class);
        when(localEvent.getEvent()).thenReturn(mock(Event.class));
        assertFalse(this.converterToRemote.toRemote(localEvent, remoteEvent));
    }

    @Test
    void fromRemoteChangeRequestCreatedEvent() throws ChangeRequestEventsConverterException
    {
        RemoteEventData remoteEvent = mock(RemoteEventData.class);
        LocalEventData localEvent = mock(LocalEventData.class);

        ChangeRequestCreatedEvent event = new ChangeRequestCreatedEvent();
        when(remoteEvent.getEvent()).thenReturn(event);
        String source = "someCr43";
        when(remoteEvent.getSource()).thenReturn(source);

        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(this.helper.getChangeRequest(source, remoteEvent)).thenReturn(changeRequest);
        assertTrue(this.converterFromRemote.fromRemote(remoteEvent, localEvent));

        verify(remoteEvent, never()).getData();
        verify(localEvent).setEvent(event);
        verify(localEvent).setSource(source);
        verify(localEvent).setData(changeRequest);
    }

    @Test
    void fromRemoteChangeRequestMergedEvent() throws ChangeRequestEventsConverterException
    {
        RemoteEventData remoteEvent = mock(RemoteEventData.class);
        LocalEventData localEvent = mock(LocalEventData.class);

        ChangeRequestMergedEvent event = new ChangeRequestMergedEvent();
        when(remoteEvent.getEvent()).thenReturn(event);
        String source = "someCr43Merged";
        when(remoteEvent.getSource()).thenReturn(source);

        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(this.helper.getChangeRequest(source, remoteEvent)).thenReturn(changeRequest);
        assertTrue(this.converterFromRemote.fromRemote(remoteEvent, localEvent));

        verify(remoteEvent, never()).getData();
        verify(localEvent).setEvent(event);
        verify(localEvent).setSource(source);
        verify(localEvent).setData(changeRequest);
    }

    @Test
    void fromRemoteChangeRequestMergedFailedEvent() throws ChangeRequestEventsConverterException
    {
        RemoteEventData remoteEvent = mock(RemoteEventData.class);
        LocalEventData localEvent = mock(LocalEventData.class);

        ChangeRequestMergeFailedEvent event = new ChangeRequestMergeFailedEvent();
        when(remoteEvent.getEvent()).thenReturn(event);
        String source = "someCr43MergedFailed";
        when(remoteEvent.getSource()).thenReturn(source);

        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(this.helper.getChangeRequest(source, remoteEvent)).thenReturn(changeRequest);
        assertTrue(this.converterFromRemote.fromRemote(remoteEvent, localEvent));

        verify(remoteEvent, never()).getData();
        verify(localEvent).setEvent(event);
        verify(localEvent).setSource(source);
        verify(localEvent).setData(changeRequest);
    }

    @Test
    void fromRemoteChangeRequestRebasedEvent() throws ChangeRequestEventsConverterException
    {
        RemoteEventData remoteEvent = mock(RemoteEventData.class);
        LocalEventData localEvent = mock(LocalEventData.class);

        ChangeRequestRebasedEvent event = new ChangeRequestRebasedEvent();
        when(remoteEvent.getEvent()).thenReturn(event);
        String source = "someCr43Rebased";
        when(remoteEvent.getSource()).thenReturn(source);

        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(this.helper.getChangeRequest(source, remoteEvent)).thenReturn(changeRequest);
        assertTrue(this.converterFromRemote.fromRemote(remoteEvent, localEvent));

        verify(remoteEvent, never()).getData();
        verify(localEvent).setEvent(event);
        verify(localEvent).setSource(source);
        verify(localEvent).setData(changeRequest);
    }

    @Test
    void fromRemoteChangeRequestUpdatedEvent() throws ChangeRequestEventsConverterException
    {
        RemoteEventData remoteEvent = mock(RemoteEventData.class);
        LocalEventData localEvent = mock(LocalEventData.class);

        ChangeRequestUpdatedEvent event = new ChangeRequestUpdatedEvent();
        when(remoteEvent.getEvent()).thenReturn(event);
        String source = "someCr43Updated";
        when(remoteEvent.getSource()).thenReturn(source);

        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(this.helper.getChangeRequest(source, remoteEvent)).thenReturn(changeRequest);
        assertTrue(this.converterFromRemote.fromRemote(remoteEvent, localEvent));

        verify(remoteEvent, never()).getData();
        verify(localEvent).setEvent(event);
        verify(localEvent).setSource(source);
        verify(localEvent).setData(changeRequest);
    }

    @Test
    void fromRemoteStaleChangeRequestEvent() throws ChangeRequestEventsConverterException
    {
        RemoteEventData remoteEvent = mock(RemoteEventData.class);
        LocalEventData localEvent = mock(LocalEventData.class);

        StaleChangeRequestEvent event = new StaleChangeRequestEvent();
        when(remoteEvent.getEvent()).thenReturn(event);
        String source = "someCr43Stale";
        when(remoteEvent.getSource()).thenReturn(source);

        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(this.helper.getChangeRequest(source, remoteEvent)).thenReturn(changeRequest);
        assertTrue(this.converterFromRemote.fromRemote(remoteEvent, localEvent));

        verify(remoteEvent, never()).getData();
        verify(localEvent).setEvent(event);
        verify(localEvent).setSource(source);
        verify(localEvent).setData(changeRequest);
    }

    @Test
    void fromRemoteChangeRequestFileChangeAddedEvent() throws ChangeRequestEventsConverterException
    {
        RemoteEventData remoteEvent = mock(RemoteEventData.class);
        LocalEventData localEvent = mock(LocalEventData.class);

        ChangeRequestFileChangeAddedEvent event = new ChangeRequestFileChangeAddedEvent();
        when(remoteEvent.getEvent()).thenReturn(event);
        String source = "someCr422FileChangeAdded";
        when(remoteEvent.getSource()).thenReturn(source);
        String fileChangeId = "fileChangeAdded";
        when(remoteEvent.getData()).thenReturn(fileChangeId);

        FileChange fileChange = mock(FileChange.class);
        when(this.helper.getFileChange(source, fileChangeId, remoteEvent)).thenReturn(fileChange);
        assertTrue(this.converterFromRemote.fromRemote(remoteEvent, localEvent));

        verify(localEvent).setEvent(event);
        verify(localEvent).setSource(source);
        verify(localEvent).setData(fileChange);
    }

    @Test
    void fromRemoteFileChangeRebasedEvent() throws ChangeRequestEventsConverterException
    {
        RemoteEventData remoteEvent = mock(RemoteEventData.class);
        LocalEventData localEvent = mock(LocalEventData.class);

        FileChangeRebasedEvent event = new FileChangeRebasedEvent();
        when(remoteEvent.getEvent()).thenReturn(event);
        String source = "someCr422FileChangeRebased";
        when(remoteEvent.getSource()).thenReturn(source);
        String fileChangeId = "fileChangeRebased";
        when(remoteEvent.getData()).thenReturn(fileChangeId);

        FileChange fileChange = mock(FileChange.class);
        when(this.helper.getFileChange(source, fileChangeId, remoteEvent)).thenReturn(fileChange);
        assertTrue(this.converterFromRemote.fromRemote(remoteEvent, localEvent));

        verify(localEvent).setEvent(event);
        verify(localEvent).setSource(source);
        verify(localEvent).setData(fileChange);
    }

    @Test
    void fromRemoteChangeRequestMergingEvent() throws ChangeRequestEventsConverterException
    {
        RemoteEventData remoteEvent = mock(RemoteEventData.class);
        LocalEventData localEvent = mock(LocalEventData.class);

        ChangeRequestMergingEvent event = new ChangeRequestMergingEvent();
        when(remoteEvent.getEvent()).thenReturn(event);
        String source = "someCr43Merging";
        when(remoteEvent.getSource()).thenReturn(source);

        assertTrue(this.converterFromRemote.fromRemote(remoteEvent, localEvent));

        verify(remoteEvent, never()).getData();
        verify(localEvent).setEvent(event);
        verify(localEvent).setSource(source);
        verify(localEvent, never()).setData(any());
    }

    @Test
    void fromRemoteChangeRequestUpdatingFileChangeEvent() throws ChangeRequestEventsConverterException
    {
        RemoteEventData remoteEvent = mock(RemoteEventData.class);
        LocalEventData localEvent = mock(LocalEventData.class);

        ChangeRequestUpdatingFileChangeEvent event = new ChangeRequestUpdatingFileChangeEvent();
        when(remoteEvent.getEvent()).thenReturn(event);
        String source = "someCr43UpdatingFileChange";
        when(remoteEvent.getSource()).thenReturn(source);

        assertTrue(this.converterFromRemote.fromRemote(remoteEvent, localEvent));

        verify(remoteEvent, never()).getData();
        verify(localEvent).setEvent(event);
        verify(localEvent).setSource(source);
        verify(localEvent, never()).setData(any());
    }

    @Test
    void fromRemoteSplitBeginChangeRequestEvent() throws ChangeRequestEventsConverterException
    {
        RemoteEventData remoteEvent = mock(RemoteEventData.class);
        LocalEventData localEvent = mock(LocalEventData.class);

        SplitBeginChangeRequestEvent event = new SplitBeginChangeRequestEvent();
        when(remoteEvent.getEvent()).thenReturn(event);
        String source = "someCr43SplitBegin";
        when(remoteEvent.getSource()).thenReturn(source);

        assertTrue(this.converterFromRemote.fromRemote(remoteEvent, localEvent));

        verify(remoteEvent, never()).getData();
        verify(localEvent).setEvent(event);
        verify(localEvent).setSource(source);
        verify(localEvent, never()).setData(any());
    }

    @Test
    void fromRemoteApproversUpdatedEvent() throws ChangeRequestEventsConverterException, XWikiException
    {
        RemoteEventData remoteEvent = mock(RemoteEventData.class);
        LocalEventData localEvent = mock(LocalEventData.class);

        ApproversUpdatedEvent event = new ApproversUpdatedEvent();
        when(remoteEvent.getEvent()).thenReturn(event);
        String source = "serializedDoc";
        when(remoteEvent.getSource()).thenReturn(source);

        Pair<HashSet<String>, HashSet<String>> data = Pair.of(
            new HashSet<>(Collections.emptySet()),
            new HashSet<>(List.of("foo", "bar"))
        );
        when(remoteEvent.getData()).thenReturn(data);

        XWikiDocument document = mock(XWikiDocument.class);
        when(this.serializer.unserializeDocument(source)).thenReturn(document);
        assertTrue(this.converterFromRemote.fromRemote(remoteEvent, localEvent));

        verify(localEvent).setEvent(event);
        verify(localEvent).setSource(document);
        verify(localEvent).setData(data);
    }

    @Test
    void fromRemoteFileChangeUpdatedEvent() throws ChangeRequestEventsConverterException
    {
        RemoteEventData remoteEvent = mock(RemoteEventData.class);
        LocalEventData localEvent = mock(LocalEventData.class);

        ChangeRequestUpdatedFileChangeEvent event = new ChangeRequestUpdatedFileChangeEvent();
        when(remoteEvent.getEvent()).thenReturn(event);
        String source = "fileChangeUpdatedCR";
        when(remoteEvent.getSource()).thenReturn(source);
        when(remoteEvent.getData()).thenReturn(null);

        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(this.helper.getChangeRequest(source, remoteEvent)).thenReturn(changeRequest);
        assertTrue(this.converterFromRemote.fromRemote(remoteEvent, localEvent));

        verify(localEvent).setEvent(event);
        verify(localEvent).setSource(source);
        verify(localEvent).setData(changeRequest);

        String fileChangeId = "someFileChangeUpdated";
        FileChange fileChange = mock(FileChange.class);
        when(remoteEvent.getData()).thenReturn(fileChangeId);
        when(this.helper.getFileChange(source, fileChangeId, remoteEvent)).thenReturn(fileChange);
        assertTrue(this.converterFromRemote.fromRemote(remoteEvent, localEvent));

        verify(localEvent, times(2)).setEvent(event);
        verify(localEvent, times(2)).setSource(source);
        verify(localEvent).setData(fileChange);
    }

    @Test
    void fromRemoteChangeRequestReviewAddedEvent() throws ChangeRequestEventsConverterException
    {
        RemoteEventData remoteEvent = mock(RemoteEventData.class);
        LocalEventData localEvent = mock(LocalEventData.class);

        ChangeRequestReviewAddedEvent event = new ChangeRequestReviewAddedEvent();
        when(remoteEvent.getEvent()).thenReturn(event);
        String source = "reviewAddedUpdatedCR";
        when(remoteEvent.getSource()).thenReturn(source);
        String data = "reviewId43";
        when(remoteEvent.getData()).thenReturn(data);

        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(this.helper.getChangeRequest(source, remoteEvent)).thenReturn(changeRequest);
        ChangeRequestReview review1 = mock(ChangeRequestReview.class);
        ChangeRequestReview review2 = mock(ChangeRequestReview.class);
        ChangeRequestReview review3 = mock(ChangeRequestReview.class);

        when(review1.getId()).thenReturn("f");
        when(review2.getId()).thenReturn(data);
        when(review1.getId()).thenReturn("e");
        when(changeRequest.getReviews()).thenReturn(List.of(review1, review2, review3));
        assertTrue(this.converterFromRemote.fromRemote(remoteEvent, localEvent));

        verify(localEvent).setEvent(event);
        verify(localEvent).setSource(source);
        verify(localEvent).setData(review2);
    }

    @Test
    void fromRemoteChangeRequestStatusChangedEvent() throws ChangeRequestEventsConverterException
    {
        RemoteEventData remoteEvent = mock(RemoteEventData.class);
        LocalEventData localEvent = mock(LocalEventData.class);

        ChangeRequestStatusChangedEvent event = new ChangeRequestStatusChangedEvent();
        when(remoteEvent.getEvent()).thenReturn(event);
        String source = "statusChangedCR";
        when(remoteEvent.getSource()).thenReturn(source);
        ArrayList<String> data = new ArrayList<>(List.of("READY_FOR_REVIEW", "READY_FOR_MERGING"));
        when(remoteEvent.getData()).thenReturn(data);
        assertTrue(this.converterFromRemote.fromRemote(remoteEvent, localEvent));

        ChangeRequestStatus[] expectedData = new ChangeRequestStatus[] {
            ChangeRequestStatus.READY_FOR_REVIEW,
            ChangeRequestStatus.READY_FOR_MERGING
        };
        verify(localEvent).setEvent(event);
        verify(localEvent).setSource(source);
        verify(localEvent).setData(expectedData);
    }

    @Test
    void fromRemoteFileChangeDocumentSavedEvent() throws ChangeRequestEventsConverterException, XWikiException
    {
        RemoteEventData remoteEvent = mock(RemoteEventData.class);
        LocalEventData localEvent = mock(LocalEventData.class);

        FileChangeDocumentSavedEvent event = new FileChangeDocumentSavedEvent();
        when(remoteEvent.getEvent()).thenReturn(event);
        String changeRequestId = "fileChangeCR";
        String fileChangeId = "fileChangeId888";
        Pair<String,String> source = Pair.of(changeRequestId, fileChangeId);
        when(remoteEvent.getSource()).thenReturn(source);
        String data = "serializedDoc988";
        when(remoteEvent.getData()).thenReturn(data);

        FileChange fileChange = mock(FileChange.class);
        when(this.helper.getFileChange(changeRequestId, fileChangeId, remoteEvent)).thenReturn(fileChange);
        XWikiDocument document = mock(XWikiDocument.class);
        when(this.serializer.unserializeDocument(data)).thenReturn(document);

        assertTrue(this.converterFromRemote.fromRemote(remoteEvent, localEvent));

        verify(localEvent).setEvent(event);
        verify(localEvent).setSource(fileChange);
        verify(localEvent).setData(document);
    }

    @Test
    void fromRemoteSplitEndChangeRequestEvent() throws ChangeRequestEventsConverterException
    {
        RemoteEventData remoteEvent = mock(RemoteEventData.class);
        LocalEventData localEvent = mock(LocalEventData.class);

        SplitEndChangeRequestEvent event = new SplitEndChangeRequestEvent();
        when(remoteEvent.getEvent()).thenReturn(event);
        String source = "originalBeforeSplitCR";
        when(remoteEvent.getSource()).thenReturn(source);
        String crId1 = "cr4551";
        String crId2 = "cr4552";
        String crId3 = "cr4553";
        when(remoteEvent.getData()).thenReturn(new ArrayList<>(List.of(crId1, crId2, crId3)));

        ChangeRequest changeRequest1 = mock(ChangeRequest.class, crId1);
        ChangeRequest changeRequest2 = mock(ChangeRequest.class, crId2);
        ChangeRequest changeRequest3 = mock(ChangeRequest.class, crId3);

        when(this.helper.getChangeRequest(crId1, remoteEvent)).thenReturn(changeRequest1);
        when(this.helper.getChangeRequest(crId2, remoteEvent)).thenReturn(changeRequest2);
        when(this.helper.getChangeRequest(crId3, remoteEvent)).thenReturn(changeRequest3);

        assertTrue(this.converterFromRemote.fromRemote(remoteEvent, localEvent));

        verify(localEvent).setEvent(event);
        verify(localEvent).setSource(source);
        verify(localEvent).setData(new ArrayList<>(List.of(changeRequest1, changeRequest2, changeRequest3)));
    }
}
