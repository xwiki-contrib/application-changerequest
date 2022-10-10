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

import org.junit.jupiter.api.Test;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestManager;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.events.ChangeRequestUpdatedFileChangeEvent;
import org.xwiki.contrib.changerequest.internal.cache.ChangeRequestStorageCacheManager;
import org.xwiki.contrib.changerequest.internal.cache.MergeCacheManager;
import org.xwiki.observation.remote.RemoteObservationManagerContext;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link FileChangeUpdatedListener}.
 *
 * @version $Id$
 * @since 0.13
 */
@ComponentTest
class FileChangeUpdatedListenerTest
{
    @InjectMockComponents
    private FileChangeUpdatedListener fileChangeUpdatedListener;

    @MockComponent
    private MergeCacheManager mergeCacheManager;

    @MockComponent
    private ChangeRequestStorageCacheManager changeRequestCacheManager;

    @MockComponent
    private ChangeRequestManager changeRequestManager;

    @MockComponent
    private RemoteObservationManagerContext remoteObservationManagerContext;

    @Test
    void onEventWithFileChange() throws ChangeRequestException
    {
        String crId = "crId42";
        FileChange data = mock(FileChange.class);

        when(this.remoteObservationManagerContext.isRemoteState()).thenReturn(true);
        this.fileChangeUpdatedListener.onEvent(null, crId, data);

        verify(this.mergeCacheManager).invalidate(data);
        verify(this.changeRequestCacheManager).invalidate(crId);
        verifyNoInteractions(this.changeRequestManager);

        when(this.remoteObservationManagerContext.isRemoteState()).thenReturn(false);
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(data.getChangeRequest()).thenReturn(changeRequest);
        this.fileChangeUpdatedListener.onEvent(new ChangeRequestUpdatedFileChangeEvent(), crId, data);

        verify(this.mergeCacheManager, times(2)).invalidate(data);
        verify(this.changeRequestCacheManager, times(2)).invalidate(crId);
        verify(this.changeRequestManager).invalidateReviews(changeRequest);
        verify(this.changeRequestManager).computeReadyForMergingStatus(changeRequest);
    }

    @Test
    void onEventWithChangeRequest() throws ChangeRequestException
    {
        String crId = "crId4552";
        ChangeRequest data = mock(ChangeRequest.class);
        FileChange fileChange1 = mock(FileChange.class);
        FileChange fileChange2 = mock(FileChange.class);
        when(data.getLastFileChanges()).thenReturn(List.of(fileChange1, fileChange2));

        when(this.remoteObservationManagerContext.isRemoteState()).thenReturn(true);
        this.fileChangeUpdatedListener.onEvent(null, crId, data);

        verify(this.mergeCacheManager).invalidate(fileChange1);
        verify(this.mergeCacheManager).invalidate(fileChange2);
        verify(this.changeRequestCacheManager).invalidate(crId);
        verifyNoInteractions(this.changeRequestManager);

        when(this.remoteObservationManagerContext.isRemoteState()).thenReturn(false);
        this.fileChangeUpdatedListener.onEvent(new ChangeRequestUpdatedFileChangeEvent(), crId, data);

        verify(this.mergeCacheManager, times(2)).invalidate(fileChange1);
        verify(this.mergeCacheManager, times(2)).invalidate(fileChange2);
        verify(this.changeRequestCacheManager, times(2)).invalidate(crId);
        verify(this.changeRequestManager).invalidateReviews(data);
        verify(this.changeRequestManager).computeReadyForMergingStatus(data);
    }
}