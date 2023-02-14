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

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.contrib.changerequest.ApproversManager;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.events.ChangeRequestCreatedEvent;
import org.xwiki.contrib.changerequest.events.SplitBeginChangeRequestEvent;
import org.xwiki.contrib.changerequest.internal.ChangeRequestAutoWatchHandler;
import org.xwiki.contrib.changerequest.internal.ChangeRequestRecordableEventNotifier;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestCreatedRecordableEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.ObservationContext;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ChangeRequestCreatedEventListener}.
 *
 * @version $Id$
 * @since 0.13
 */
@ComponentTest
class ChangeRequestCreatedEventListenerTest
{
    @InjectMockComponents
    private ChangeRequestCreatedEventListener listener;

    @MockComponent
    private ChangeRequestAutoWatchHandler changeRequestAutoWatchHandler;

    @MockComponent
    private ApproversManager<ChangeRequest> changeRequestApproversManager;

    @MockComponent
    private ChangeRequestRecordableEventNotifier changeRequestRecordableEventNotifier;

    @MockComponent
    private ObservationContext observationContext;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @Test
    void onEvent() throws Exception
    {
        String source = "crId";
        ChangeRequest data = mock(ChangeRequest.class);
        UserReference creator = mock(UserReference.class);
        when(data.getCreator()).thenReturn(creator);
        when(changeRequestAutoWatchHandler.shouldCreateWatchedEntity(data, creator)).thenReturn(true);
        FileChange fileChange = mock(FileChange.class);
        String fileChangeId = "fileChangeId";
        when(fileChange.getId()).thenReturn(fileChangeId);
        when(data.getAllFileChanges()).thenReturn(Collections.singletonList(fileChange));

        UserReference approver1 = mock(UserReference.class);
        UserReference approver2 = mock(UserReference.class);
        UserReference approver3 = mock(UserReference.class);
        when(this.changeRequestApproversManager.getAllApprovers(data, false)).thenReturn(Set.of(
            approver1,
            approver2,
            approver3
        ));
        when(changeRequestAutoWatchHandler.shouldCreateWatchedEntity(data, approver1)).thenReturn(true);
        when(changeRequestAutoWatchHandler.shouldCreateWatchedEntity(data, approver2)).thenReturn(false);
        when(changeRequestAutoWatchHandler.shouldCreateWatchedEntity(data, approver3)).thenReturn(true);

        DocumentReference documentReference = mock(DocumentReference.class);
        when(data.getModifiedDocuments()).thenReturn(Collections.singleton(documentReference));

        XWikiDocument document = mock(XWikiDocument.class);
        when(this.documentAccessBridge.getTranslatedDocumentInstance(documentReference)).thenReturn(document);

        when(observationContext.isIn(any(SplitBeginChangeRequestEvent.class))).thenReturn(true);

        doAnswer(invocation -> {
            ChangeRequestCreatedRecordableEvent event = invocation.getArgument(0);
            assertTrue(event.isFromSplit());
            assertEquals(fileChangeId, event.getFileChangeId());
            return null;
        }).when(this.changeRequestRecordableEventNotifier).notifyChangeRequestRecordableEvent(
            any(ChangeRequestCreatedRecordableEvent.class),
            eq(document));

        this.listener.onEvent(new ChangeRequestCreatedEvent(), source, data);
        verify(this.changeRequestRecordableEventNotifier).notifyChangeRequestRecordableEvent(
            any(ChangeRequestCreatedRecordableEvent.class),
            eq(document));
        verify(changeRequestAutoWatchHandler).watchChangeRequest(data, creator);
        verify(changeRequestAutoWatchHandler).watchChangeRequest(data, approver1);
        verify(changeRequestAutoWatchHandler).watchChangeRequest(data, approver3);
        verify(changeRequestAutoWatchHandler, never()).watchChangeRequest(data, approver2);
    }
}
