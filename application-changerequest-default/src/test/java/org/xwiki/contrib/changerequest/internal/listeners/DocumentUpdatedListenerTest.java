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

import javax.inject.Inject;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestManager;
import org.xwiki.contrib.changerequest.ChangeRequestStatus;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.job.Job;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    private XWikiContext context;

    @BeforeEach
    void setup()
    {
        this.context = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(this.context);
    }

    @Test
    void processLocalEvent() throws ChangeRequestException
    {
        XWikiDocument sourceDoc = mock(XWikiDocument.class);
        DocumentReference documentReference = new DocumentReference("foo", "XWiki", "Document");
        when(sourceDoc.getDocumentReferenceWithLocale()).thenReturn(documentReference);
        when(this.context.getMainXWiki()).thenReturn("foo");

        ChangeRequest changeRequest1 = mock(ChangeRequest.class);
        ChangeRequest changeRequest2 = mock(ChangeRequest.class);

        when(this.storageManager.findChangeRequestTargeting(documentReference)).thenReturn(Arrays.asList(
            changeRequest1,
            changeRequest2
        ));
        when(changeRequest1.getStatus()).thenReturn(ChangeRequestStatus.MERGED);
        when(changeRequest2.getStatus()).thenReturn(ChangeRequestStatus.READY_FOR_MERGING);

        this.listener.processLocalEvent(new DocumentUpdatedEvent(), sourceDoc, null);

        verify(this.changeRequestManager, never()).computeReadyForMergingStatus(changeRequest1);
        verify(this.changeRequestManager).computeReadyForMergingStatus(changeRequest2);

        when(this.context.getMainXWiki()).thenReturn("bar");
        XWiki wiki = mock(XWiki.class);
        when(context.getWiki()).thenReturn(wiki);
        when(wiki.getWikiInitializerJob("foo")).thenReturn(null);

        // this will do nothing
        this.listener.processLocalEvent(new DocumentUpdatedEvent(), sourceDoc, null);

        verify(this.changeRequestManager, never()).computeReadyForMergingStatus(changeRequest1);
        verify(this.changeRequestManager).computeReadyForMergingStatus(changeRequest2);

        Job job = mock(Job.class);
        when(wiki.getWikiInitializerJob("foo")).thenReturn(job);
        JobStatus jobStatus = mock(JobStatus.class);
        when(job.getStatus()).thenReturn(jobStatus);
        when(jobStatus.getState()).thenReturn(JobStatus.State.RUNNING);

        // this will do nothing
        this.listener.processLocalEvent(new DocumentUpdatedEvent(), sourceDoc, null);

        verify(this.changeRequestManager, never()).computeReadyForMergingStatus(changeRequest1);
        verify(this.changeRequestManager).computeReadyForMergingStatus(changeRequest2);

        when(jobStatus.getState()).thenReturn(JobStatus.State.FINISHED);

        // this will be process again the event
        this.listener.processLocalEvent(new DocumentUpdatedEvent(), sourceDoc, null);

        verify(this.changeRequestManager, never()).computeReadyForMergingStatus(changeRequest1);
        verify(this.changeRequestManager, times(2)).computeReadyForMergingStatus(changeRequest2);
    }
}
