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
package org.xwiki.contrib.changerequest.replication.internal.listeners;

import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.notifications.events.DocumentModifiedInChangeRequestEvent;
import org.xwiki.contrib.changerequest.replication.internal.messages.ChangeRequestReplicationSenderMessage;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.contrib.replication.ReplicationContext;
import org.xwiki.contrib.replication.entity.DocumentReplicationLevel;
import org.xwiki.contrib.replication.entity.DocumentReplicationSender;
import org.xwiki.contrib.replication.entity.ReplicationSenderMessageProducer;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.observation.remote.RemoteObservationManagerContext;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DocumentModifiedInChangeRequestListener}.
 *
 * @version $Id$
 */
@ComponentTest
class DocumentModifiedInChangeRequestListenerTest
{
    @InjectMockComponents
    private DocumentModifiedInChangeRequestListener listener;

    @MockComponent
    private DocumentReplicationSender documentReplicationSender;

    @MockComponent
    private RemoteObservationManagerContext remoteObservationManagerContext;

    @MockComponent
    private ReplicationContext replicationContext;

    @MockComponent
    private ChangeRequestStorageManager changeRequestStorageManager;

    @MockComponent
    private DocumentReferenceResolver<ChangeRequest> changeRequestDocumentReferenceResolver;

    @BeforeComponent
    void beforeComponent(MockitoComponentManager componentManager) throws Exception
    {
        componentManager.registerComponent(ComponentManager.class, "context", componentManager);
    }

    @Test
    void getEvents()
    {
        assertEquals(1, listener.getEvents().size());
        assertTrue(listener.getEvents().get(0) instanceof DocumentModifiedInChangeRequestEvent);
    }

    @Test
    void onEvent(MockitoComponentManager componentManager) throws Exception
    {
        String crId = "crFooBar";
        DocumentModifiedInChangeRequestEvent event = new DocumentModifiedInChangeRequestEvent(crId);
        XWikiDocument data = mock(XWikiDocument.class, "data");
        DocumentReference dataDocRef = mock(DocumentReference.class);
        when(data.getDocumentReference()).thenReturn(dataDocRef);

        String expectedHint = DocumentModifiedInChangeRequestEvent.EVENT_NAME;

        when(this.replicationContext.isReplicationMessage()).thenReturn(true);
        this.listener.onEvent(event, null, data);
        verifyNoInteractions(this.documentReplicationSender);

        when(this.remoteObservationManagerContext.isRemoteState()).thenReturn(true);
        when(this.replicationContext.isReplicationMessage()).thenReturn(false);
        this.listener.onEvent(event, null, data);
        verifyNoInteractions(this.documentReplicationSender);

        when(this.remoteObservationManagerContext.isRemoteState()).thenReturn(false);

        ChangeRequestReplicationSenderMessage senderMessage =
            componentManager.registerMockComponent(ChangeRequestReplicationSenderMessage.class, expectedHint);


        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(this.changeRequestStorageManager.load(crId)).thenReturn(Optional.of(changeRequest));
        DocumentReference crDocReference = mock(DocumentReference.class, "crDoc");

        when(this.changeRequestDocumentReferenceResolver.resolve(changeRequest)).thenReturn(crDocReference);
        doAnswer(invocationOnMock -> {
            ReplicationSenderMessageProducer producer = invocationOnMock.getArgument(0);
            assertEquals(senderMessage, producer.produce(null));
            return null;
        }).when(this.documentReplicationSender)
            .send(any(), eq(crDocReference), eq(DocumentReplicationLevel.ALL), eq(Collections.emptyMap()), isNull());

        this.listener.onEvent(event, null, data);
        verify(senderMessage).initialize(event, dataDocRef);
        verify(this.documentReplicationSender).send(any(), eq(crDocReference), eq(DocumentReplicationLevel.ALL),
            eq(Collections.emptyMap()), isNull());
    }
}