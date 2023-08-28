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
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.events.ChangeRequestMergedEvent;
import org.xwiki.contrib.changerequest.internal.ChangeRequestAutoWatchHandler;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.filters.watch.AutomaticWatchMode;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ChangeRequestMergedEventListener}.
 *
 * @version $Id$
 */
@ComponentTest
class ChangeRequestMergedEventListenerTest
{
    @InjectMockComponents
    private ChangeRequestMergedEventListener changeRequestMergedEventListener;

    @MockComponent
    private ChangeRequestAutoWatchHandler autoWatchHandler;

    @Test
    void onEvent() throws ChangeRequestException
    {
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        UserReference user1 = mock(UserReference.class, "user1");
        UserReference user2 = mock(UserReference.class, "user2");
        UserReference user3 = mock(UserReference.class, "user3");

        FileChange fileChange1 = mock(FileChange.class, "fileChange1");
        FileChange fileChange2 = mock(FileChange.class, "fileChange2");
        FileChange fileChange3 = mock(FileChange.class, "fileChange3");

        when(this.autoWatchHandler.getAutomaticWatchMode(user1)).thenReturn(AutomaticWatchMode.NEW);
        when(this.autoWatchHandler.getAutomaticWatchMode(user2)).thenReturn(AutomaticWatchMode.NONE);
        when(this.autoWatchHandler.getAutomaticWatchMode(user3)).thenReturn(AutomaticWatchMode.ALL);

        when(fileChange1.getType()).thenReturn(FileChange.FileChangeType.EDITION);
        when(fileChange2.getType()).thenReturn(FileChange.FileChangeType.DELETION);
        when(fileChange3.getType()).thenReturn(FileChange.FileChangeType.CREATION);

        DocumentReference entity1 = mock(DocumentReference.class, "ref1");
        DocumentReference entity2 = mock(DocumentReference.class, "ref2");
        DocumentReference entity3 = mock(DocumentReference.class, "ref3");
        when(fileChange1.getTargetEntity()).thenReturn(entity1);
        when(fileChange2.getTargetEntity()).thenReturn(entity2);
        when(fileChange3.getTargetEntity()).thenReturn(entity3);

        when(changeRequest.getAuthors()).thenReturn(Set.of(user1, user2, user3));
        when(changeRequest.getLastFileChanges()).thenReturn(List.of(fileChange1, fileChange2, fileChange3));

        this.changeRequestMergedEventListener.onEvent(new ChangeRequestMergedEvent(), null, changeRequest);
        verify(this.autoWatchHandler, never()).watchDocument(entity1, user1);
        verify(this.autoWatchHandler, never()).watchDocument(entity1, user2);
        verify(this.autoWatchHandler).watchDocument(entity1, user3);

        verify(this.autoWatchHandler, never()).watchDocument(entity2, user1);
        verify(this.autoWatchHandler, never()).watchDocument(entity2, user2);
        verify(this.autoWatchHandler, never()).watchDocument(entity2, user3);

        verify(this.autoWatchHandler).watchDocument(entity3, user1);
        verify(this.autoWatchHandler, never()).watchDocument(entity3, user2);
        verify(this.autoWatchHandler).watchDocument(entity3, user3);
    }
}