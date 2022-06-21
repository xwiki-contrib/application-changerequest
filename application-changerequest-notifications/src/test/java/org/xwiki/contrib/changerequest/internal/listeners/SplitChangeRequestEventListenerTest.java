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

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.events.SplitChangeRequestEvent;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestCreatedRecordableEvent;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.observation.ObservationManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link SplitChangeRequestEventListener}.
 *
 * @version $Id$
 * @since 0.14
 */
@ComponentTest
class SplitChangeRequestEventListenerTest
{
    @InjectMockComponents
    private SplitChangeRequestEventListener listener;

    @MockComponent
    private ChangeRequestStorageManager changeRequestStorageManager;

    @MockComponent
    private DocumentReferenceResolver<ChangeRequest> changeRequestDocumentReferenceResolver;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @MockComponent
    private ObservationManager observationManager;

    @Test
    void onEvent() throws Exception
    {
        SplitChangeRequestEvent event = mock(SplitChangeRequestEvent.class);
        ChangeRequest changeRequest1 = mock(ChangeRequest.class, "cr1");
        ChangeRequest changeRequest2 = mock(ChangeRequest.class, "cr2");
        List<ChangeRequest> data = List.of(changeRequest1, changeRequest2);

        String cr1Id = "cr1";
        when(changeRequest1.getId()).thenReturn(cr1Id);
        String cr2Id = "cr2";
        when(changeRequest2.getId()).thenReturn(cr2Id);
        when(this.changeRequestStorageManager.load(cr1Id)).thenReturn(Optional.of(changeRequest1));
        when(this.changeRequestStorageManager.load(cr2Id)).thenReturn(Optional.of(changeRequest2));

        DocumentReference refCR1 = mock(DocumentReference.class, "RefCR1");
        DocumentReference refCR2 = mock(DocumentReference.class, "RefCR2");
        when(this.changeRequestDocumentReferenceResolver.resolve(changeRequest1)).thenReturn(refCR1);
        when(this.changeRequestDocumentReferenceResolver.resolve(changeRequest2)).thenReturn(refCR2);

        XWikiDocument docCR1 = mock(XWikiDocument.class, "docCR1");
        XWikiDocument docCR2 = mock(XWikiDocument.class, "docCR2");
        when(this.documentAccessBridge.getTranslatedDocumentInstance(refCR1)).thenReturn(docCR1);
        when(this.documentAccessBridge.getTranslatedDocumentInstance(refCR2)).thenReturn(docCR2);

        doAnswer(invocation -> {
            ChangeRequestCreatedRecordableEvent recordableEvent = invocation.getArgument(0);
            assertTrue(recordableEvent.isFromSplit());
            return null;
        }).when(this.observationManager).notify(any(ChangeRequestCreatedRecordableEvent.class), any(), any());

        this.listener.onEvent(event, "foo", data);

        verify(this.observationManager).notify(any(ChangeRequestCreatedRecordableEvent.class),
            eq(AbstractChangeRequestEventListener.EVENT_SOURCE), eq(docCR1));
        verify(this.observationManager).notify(any(ChangeRequestCreatedRecordableEvent.class),
            eq(AbstractChangeRequestEventListener.EVENT_SOURCE), eq(docCR2));
    }
}