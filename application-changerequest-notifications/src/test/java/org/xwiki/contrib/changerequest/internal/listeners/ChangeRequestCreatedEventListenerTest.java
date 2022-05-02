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

import javax.inject.Provider;

import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.events.ChangeRequestCreatedEvent;
import org.xwiki.contrib.changerequest.internal.ChangeRequestAutoWatchHandler;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestCreatedRecordableEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
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
    private Provider<ChangeRequestAutoWatchHandler> autoWatchHandlerProvider;

    @MockComponent
    private ObservationManager observationManager;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @Test
    void onEvent() throws Exception
    {
        String source = "crId";
        ChangeRequest data = mock(ChangeRequest.class);

        ChangeRequestAutoWatchHandler changeRequestAutoWatchHandler = mock(ChangeRequestAutoWatchHandler.class);
        when(this.autoWatchHandlerProvider.get()).thenReturn(changeRequestAutoWatchHandler);

        when(changeRequestAutoWatchHandler.shouldCreateWatchedEntity(data)).thenReturn(true);
        DocumentReference documentReference = mock(DocumentReference.class);
        when(data.getModifiedDocuments()).thenReturn(Collections.singleton(documentReference));

        XWikiDocument document = mock(XWikiDocument.class);
        when(this.documentAccessBridge.getTranslatedDocumentInstance(documentReference)).thenReturn(document);

        doAnswer(invocation -> {
            ChangeRequestCreatedRecordableEvent event = invocation.getArgument(0);
            assertEquals(source, event.getChangeRequestId());
            return null;
        }).when(this.observationManager).notify(any(ChangeRequestCreatedRecordableEvent.class),
            eq(AbstractChangeRequestEventListener.EVENT_SOURCE), eq(document));

        this.listener.onEvent(new ChangeRequestCreatedEvent(), source, data);
        verify(this.observationManager).notify(any(ChangeRequestCreatedRecordableEvent.class),
            eq(AbstractChangeRequestEventListener.EVENT_SOURCE), eq(document));
    }
}
