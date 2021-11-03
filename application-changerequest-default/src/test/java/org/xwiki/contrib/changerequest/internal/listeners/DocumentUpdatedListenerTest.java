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

import org.junit.jupiter.api.Test;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestManager;
import org.xwiki.contrib.changerequest.internal.listeners.DocumentUpdatedListener;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DocumentUpdatedListener}.
 *
 * @version $Id$
 * @since 0.6
 */
@ComponentTest
class DocumentUpdatedListenerTest
{
    @InjectMockComponents
    private DocumentUpdatedListener listener;

    @MockComponent
    private ChangeRequestStorageManager storageManager;

    @MockComponent
    private ChangeRequestManager changeRequestManager;

    @Test
    void processLocalEvent() throws ChangeRequestException
    {
        XWikiDocument sourceDoc = mock(XWikiDocument.class);
        DocumentReference documentReference = mock(DocumentReference.class);
        when(sourceDoc.getDocumentReferenceWithLocale()).thenReturn(documentReference);
        ChangeRequest changeRequest1 = mock(ChangeRequest.class);
        ChangeRequest changeRequest2 = mock(ChangeRequest.class);

        when(this.storageManager.findChangeRequestTargeting(documentReference)).thenReturn(Arrays.asList(
            changeRequest1,
            changeRequest2
        ));

        this.listener.processLocalEvent(new DocumentUpdatedEvent(), sourceDoc, null);
        verify(this.changeRequestManager).computeReadyForMergingStatus(changeRequest1);
        verify(this.changeRequestManager).computeReadyForMergingStatus(changeRequest2);
    }
}
