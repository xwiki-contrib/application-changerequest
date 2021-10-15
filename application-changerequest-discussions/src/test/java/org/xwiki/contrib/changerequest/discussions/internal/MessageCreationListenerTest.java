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

import javax.inject.Inject;
import javax.inject.Provider;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.xwiki.contrib.changerequest.discussions.ChangeRequestDiscussionEvent;
import org.xwiki.contrib.changerequest.discussions.ChangeRequestDiscussionException;
import org.xwiki.contrib.changerequest.discussions.ChangeRequestDiscussionService;
import org.xwiki.contrib.changerequest.discussions.references.AbstractChangeRequestDiscussionContextReference;
import org.xwiki.contrib.discussions.domain.Discussion;
import org.xwiki.contrib.discussions.domain.Message;
import org.xwiki.contrib.discussions.events.MessageEvent;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.remote.RemoteObservationManagerContext;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link MessageCreationListener}.
 *
 * @version $Id$
 */
@ComponentTest
class MessageCreationListenerTest
{
    @InjectMockComponents
    private MessageCreationListener listener;

    @MockComponent
    private Provider<ChangeRequestDiscussionService> changeRequestDiscussionServiceProvider;

    @MockComponent
    private ObservationManager observationManager;

    @MockComponent
    private RemoteObservationManagerContext remoteObservationManagerContext;

    @Test
    void onEvent() throws ChangeRequestDiscussionException
    {
        MessageEvent event = mock(MessageEvent.class);
        Message message = mock(Message.class);
        String hint = ChangeRequestDiscussionService.APPLICATION_HINT;

        when(this.remoteObservationManagerContext.isRemoteState()).thenReturn(true);
        this.listener.onEvent(event, hint, message);
        verifyNoInteractions(this.observationManager);
        verifyNoInteractions(this.changeRequestDiscussionServiceProvider);

        when(this.remoteObservationManagerContext.isRemoteState()).thenReturn(false);
        this.listener.onEvent(event, "something", message);
        verifyNoInteractions(this.observationManager);
        verifyNoInteractions(this.changeRequestDiscussionServiceProvider);

        Discussion discussion = mock(Discussion.class);
        when(message.getDiscussion()).thenReturn(discussion);
        ChangeRequestDiscussionService changeRequestDiscussionService = mock(ChangeRequestDiscussionService.class);
        when(changeRequestDiscussionServiceProvider.get()).thenReturn(changeRequestDiscussionService);
        AbstractChangeRequestDiscussionContextReference reference =
            mock(AbstractChangeRequestDiscussionContextReference.class);
        when(changeRequestDiscussionService.getReferenceFrom(discussion)).thenReturn(reference);

        when(reference.getChangeRequestId()).thenReturn("CRID");
        this.listener.onEvent(event, hint, message);
        verify(this.observationManager).notify(any(ChangeRequestDiscussionEvent.class), eq("CRID"), eq(reference));
    }
}
