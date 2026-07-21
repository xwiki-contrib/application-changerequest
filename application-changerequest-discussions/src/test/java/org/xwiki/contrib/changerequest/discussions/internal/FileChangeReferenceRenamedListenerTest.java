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
package org.xwiki.contrib.changerequest.discussions.internal;

import java.util.Locale;

import javax.inject.Provider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.contrib.changerequest.discussions.ChangeRequestDiscussionException;
import org.xwiki.contrib.changerequest.discussions.ChangeRequestDiscussionService;
import org.xwiki.contrib.changerequest.events.FileChangeReferenceRenamedEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.remote.RemoteObservationManagerContext;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link FileChangeReferenceRenamedListener}.
 *
 * @version $Id$
 */
@ComponentTest
class FileChangeReferenceRenamedListenerTest
{
    @InjectMockComponents
    private FileChangeReferenceRenamedListener listener;

    @MockComponent
    private Provider<ChangeRequestDiscussionService> discussionServiceProvider;

    @MockComponent
    private RemoteObservationManagerContext remoteObservationManagerContext;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.ERROR);

    @Test
    void onEventWhenRemoteStateDoesNothing()
    {
        when(this.remoteObservationManagerContext.isRemoteState()).thenReturn(true);
        this.listener.onEvent(new FileChangeReferenceRenamedEvent(), "CRID", null);
        verifyNoInteractions(this.discussionServiceProvider);
    }

    @Test
    void onEventRefactorsDiscussionFileReference() throws ChangeRequestDiscussionException
    {
        when(this.remoteObservationManagerContext.isRemoteState()).thenReturn(false);
        ChangeRequestDiscussionService discussionService = mock(ChangeRequestDiscussionService.class);
        when(this.discussionServiceProvider.get()).thenReturn(discussionService);

        DocumentReference source = new DocumentReference("wiki", "Space", "Source");
        DocumentReference target = new DocumentReference("wiki", "Space", "Target", Locale.ROOT);
        FileChangeReferenceRenamedEvent event = new FileChangeReferenceRenamedEvent(source, target, true);

        this.listener.onEvent(event, "CRID", null);

        DocumentReference expectedTarget = new DocumentReference(target, (Locale) null);
        verify(discussionService).refactorDiscussionFileReference("CRID", source, expectedTarget, true);
    }

    @Test
    void onEventWhenExceptionDoesNotThrow() throws ChangeRequestDiscussionException
    {
        when(this.remoteObservationManagerContext.isRemoteState()).thenReturn(false);
        ChangeRequestDiscussionService discussionService = mock(ChangeRequestDiscussionService.class);
        when(this.discussionServiceProvider.get()).thenReturn(discussionService);

        DocumentReference source = new DocumentReference("wiki", "Space", "Source");
        DocumentReference target = new DocumentReference("wiki", "Space", "Target");
        FileChangeReferenceRenamedEvent event = new FileChangeReferenceRenamedEvent(source, target, false);

        doThrow(new ChangeRequestDiscussionException("error"))
            .when(discussionService).refactorDiscussionFileReference(eq("CRID"), eq(source), eq(target), eq(false));

        this.listener.onEvent(event, "CRID", null);

        verify(discussionService).refactorDiscussionFileReference("CRID", source, target, false);
        assertEquals(1, this.logCapture.size());
        assertEquals("Error while trying to refactor discussions of change request [CRID] for the move from "
            + "[wiki:Space.Source] to [wiki:Space.Target] (isDeep: [false])", this.logCapture.getMessage(0));
    }
}
