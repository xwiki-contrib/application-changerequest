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

import java.util.Optional;

import javax.inject.Provider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.discussions.ChangeRequestDiscussionException;
import org.xwiki.contrib.changerequest.discussions.ChangeRequestDiscussionService;
import org.xwiki.contrib.changerequest.discussions.references.AbstractChangeRequestDiscussionContextReference;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.contrib.discussions.domain.Discussion;
import org.xwiki.contrib.discussions.domain.Message;
import org.xwiki.contrib.discussions.events.MessageEvent;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.observation.remote.RemoteObservationManagerContext;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link MessageEventListener}.
 *
 * @version $Id$
 */
@ComponentTest
class MessageEventListenerTest
{
    private static final String APPLICATION_HINT = ChangeRequestDiscussionService.APPLICATION_HINT;

    @InjectMockComponents
    private MessageEventListener listener;

    @MockComponent
    private Provider<ChangeRequestDiscussionService> discussionServiceProvider;

    @MockComponent
    private Provider<ChangeRequestStorageManager> changeRequestStorageManagerProvider;

    @MockComponent
    private Provider<ContextualLocalizationManager> contextualLocalizationManagerProvider;

    @MockComponent
    private RemoteObservationManagerContext remoteObservationManagerContext;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @Test
    void onEventWhenWrongApplicationHintDoesNothing()
    {
        Message message = mock(Message.class);
        this.listener.onEvent(mock(MessageEvent.class), "somethingelse", message);
        verifyNoInteractions(this.discussionServiceProvider);
        verifyNoInteractions(this.changeRequestStorageManagerProvider);
    }

    @Test
    void onEventSavesChangeRequestWhenFound() throws ChangeRequestDiscussionException, ChangeRequestException
    {
        Message message = mock(Message.class);
        Discussion discussion = mock(Discussion.class);
        when(message.getDiscussion()).thenReturn(discussion);

        ChangeRequestDiscussionService discussionService = mock(ChangeRequestDiscussionService.class);
        when(this.discussionServiceProvider.get()).thenReturn(discussionService);
        AbstractChangeRequestDiscussionContextReference reference =
            mock(AbstractChangeRequestDiscussionContextReference.class);
        when(discussionService.getReferenceFrom(discussion)).thenReturn(reference);
        when(reference.getChangeRequestId()).thenReturn("CRID");

        ChangeRequestStorageManager storageManager = mock(ChangeRequestStorageManager.class);
        when(this.changeRequestStorageManagerProvider.get()).thenReturn(storageManager);
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(storageManager.load("CRID")).thenReturn(Optional.of(changeRequest));

        ContextualLocalizationManager localizationManager = mock(ContextualLocalizationManager.class);
        when(this.contextualLocalizationManagerProvider.get()).thenReturn(localizationManager);
        when(localizationManager.getTranslationPlain("changerequest.save.messagecreated")).thenReturn("save comment");

        this.listener.onEvent(mock(MessageEvent.class), APPLICATION_HINT, message);

        verify(changeRequest).updateDate();
        verify(storageManager).save(changeRequest, "save comment");
    }

    @Test
    void onEventWhenChangeRequestNotFoundLogsWarning() throws ChangeRequestDiscussionException,
        ChangeRequestException
    {
        Message message = mock(Message.class);
        Discussion discussion = mock(Discussion.class);
        when(message.getDiscussion()).thenReturn(discussion);

        ChangeRequestDiscussionService discussionService = mock(ChangeRequestDiscussionService.class);
        when(this.discussionServiceProvider.get()).thenReturn(discussionService);
        AbstractChangeRequestDiscussionContextReference reference =
            mock(AbstractChangeRequestDiscussionContextReference.class);
        when(discussionService.getReferenceFrom(discussion)).thenReturn(reference);
        when(reference.getChangeRequestId()).thenReturn("CRID");

        ChangeRequestStorageManager storageManager = mock(ChangeRequestStorageManager.class);
        when(this.changeRequestStorageManagerProvider.get()).thenReturn(storageManager);
        when(storageManager.load("CRID")).thenReturn(Optional.empty());

        this.listener.onEvent(mock(MessageEvent.class), APPLICATION_HINT, message);

        verify(storageManager, never()).save(any(), any());
        assertEquals(1, this.logCapture.size());
        assertEquals("No change request found with id [CRID]", this.logCapture.getMessage(0));
    }

    @Test
    void onEventWhenDiscussionExceptionDoesNotThrow() throws ChangeRequestDiscussionException
    {
        Message message = mock(Message.class);
        Discussion discussion = mock(Discussion.class);
        when(message.getDiscussion()).thenReturn(discussion);

        ChangeRequestDiscussionService discussionService = mock(ChangeRequestDiscussionService.class);
        when(this.discussionServiceProvider.get()).thenReturn(discussionService);
        when(discussionService.getReferenceFrom(discussion))
            .thenThrow(new ChangeRequestDiscussionException("error"));

        this.listener.onEvent(mock(MessageEvent.class), APPLICATION_HINT, message);

        verifyNoInteractions(this.changeRequestStorageManagerProvider);
        assertEquals(1, this.logCapture.size());
        assertEquals("Error when trying to get reference from discussion [" + discussion + "]",
            this.logCapture.getMessage(0));
    }

    @Test
    void onEventWhenChangeRequestExceptionDoesNotThrow() throws ChangeRequestDiscussionException,
        ChangeRequestException
    {
        Message message = mock(Message.class);
        Discussion discussion = mock(Discussion.class);
        when(message.getDiscussion()).thenReturn(discussion);

        ChangeRequestDiscussionService discussionService = mock(ChangeRequestDiscussionService.class);
        when(this.discussionServiceProvider.get()).thenReturn(discussionService);
        AbstractChangeRequestDiscussionContextReference reference =
            mock(AbstractChangeRequestDiscussionContextReference.class);
        when(discussionService.getReferenceFrom(discussion)).thenReturn(reference);
        when(reference.getChangeRequestId()).thenReturn("CRID");

        ChangeRequestStorageManager storageManager = mock(ChangeRequestStorageManager.class);
        when(this.changeRequestStorageManagerProvider.get()).thenReturn(storageManager);
        when(storageManager.load("CRID")).thenThrow(new ChangeRequestException("error"));

        this.listener.onEvent(mock(MessageEvent.class), APPLICATION_HINT, message);

        verify(storageManager, never()).save(any(), any());
        assertEquals(1, this.logCapture.size());
        assertEquals("Error when trying to load or save change request [CRID]", this.logCapture.getMessage(0));
    }
}
