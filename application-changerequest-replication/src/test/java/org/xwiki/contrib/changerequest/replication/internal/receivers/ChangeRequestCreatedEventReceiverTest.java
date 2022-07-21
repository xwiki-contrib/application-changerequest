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
package org.xwiki.contrib.changerequest.replication.internal.receivers;

import java.util.Optional;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.internal.ChangeRequestAutoWatchHandler;
import org.xwiki.contrib.changerequest.internal.ChangeRequestRecordableEventNotifier;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestCreatedRecordableEvent;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.contrib.replication.ReplicationMessageReader;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ChangeRequestCreatedEventReceiver}.
 *
 * @version $Id$
 */
@ComponentTest
class ChangeRequestCreatedEventReceiverTest
{
    @InjectMockComponents
    private ChangeRequestCreatedEventReceiver eventReceiver;

    @MockComponent
    protected ChangeRequestRecordableEventNotifier recordableEventNotifier;

    @MockComponent
    protected DocumentAccessBridge documentAccessBridge;

    @MockComponent
    private DocumentReferenceResolver<String> stringDocumentReferenceResolver;

    @MockComponent
    private ReplicationMessageReader messageReader;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private ChangeRequestStorageManager changeRequestStorageManager;

    @MockComponent
    private ChangeRequestAutoWatchHandler autoWatchHandler;

    private XWikiContext context;

    private DocumentReference originalUserRef;

    @BeforeEach
    void setup()
    {
        this.context = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(this.context);

        this.originalUserRef = mock(DocumentReference.class, "originalUserRef");
        when(this.context.getUserReference()).thenReturn(originalUserRef);
    }

    @Test
    void receive() throws Exception
    {
        ReplicationReceiverMessage message = mock(ReplicationReceiverMessage.class);
        String contextUser = "XWiki.ContextUser";
        when(this.messageReader.getMetadata(message, "CONTEXT_USER", true)).thenReturn(contextUser);
        DocumentReference contextUserRef = mock(DocumentReference.class, "contextUserRef");
        when(this.stringDocumentReferenceResolver.resolve(contextUser)).thenReturn(contextUserRef);

        String crId = "cr243";
        when(this.messageReader.getMetadata(message, "CHANGE_REQUEST_ID", true)).thenReturn(crId);
        ChangeRequest changeRequest = mock(ChangeRequest.class);

        when(this.changeRequestStorageManager.load(crId)).thenReturn(Optional.of(changeRequest));
        when(this.autoWatchHandler.shouldCreateWatchedEntity(changeRequest)).thenReturn(true);

        when(this.messageReader.getMetadata(message, "FROM_SPLIT", true)).thenReturn("true");

        String dataDocSerializedRef = "dataDocRef";
        when(this.messageReader.getMetadata(message, "DATA_DOCUMENT", true)).thenReturn(dataDocSerializedRef);

        DocumentReference dataDocRef = mock(DocumentReference.class, "dataDocRef");
        when(this.stringDocumentReferenceResolver.resolve(dataDocSerializedRef)).thenReturn(dataDocRef);

        DocumentModelBridge dataDoc = mock(DocumentModelBridge.class, "dataDoc");
        when(this.documentAccessBridge.getTranslatedDocumentInstance(dataDocRef)).thenReturn(dataDoc);

        doAnswer(invocation -> {
            ChangeRequestCreatedRecordableEvent event = invocation.getArgument(0);
            assertEquals(crId, event.getChangeRequestId());
            assertTrue(event.isFromSplit());
            return null;
        }).when(this.recordableEventNotifier)
            .notifyChangeRequestRecordableEvent(any(ChangeRequestCreatedRecordableEvent.class), eq(dataDoc));

        this.eventReceiver.receive(message);
        verify(this.context).setUserReference(contextUserRef);
        verify(this.context).setUserReference(originalUserRef);
        verify(this.recordableEventNotifier)
            .notifyChangeRequestRecordableEvent(any(ChangeRequestCreatedRecordableEvent.class), eq(dataDoc));
    }
}