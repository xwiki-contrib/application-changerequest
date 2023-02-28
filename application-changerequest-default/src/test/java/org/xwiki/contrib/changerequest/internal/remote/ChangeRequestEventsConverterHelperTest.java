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
package org.xwiki.contrib.changerequest.internal.remote;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.internal.cache.ChangeRequestStorageCacheManager;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.observation.remote.RemoteEventData;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ChangeRequestEventsConverterHelper}.
 *
 * @version $Id$
 * @since 1.5
 */
@ComponentTest
class ChangeRequestEventsConverterHelperTest
{
    @InjectMockComponents
    private ChangeRequestEventsConverterHelper helper;

    @MockComponent
    private ChangeRequestStorageCacheManager changeRequestCacheManager;

    @MockComponent
    private ChangeRequestStorageManager changeRequestStorageManager;

    @Test
    void getChangeRequest() throws ChangeRequestException, ChangeRequestEventsConverterException
    {
        String changeRequestId = "someCRID";
        RemoteEventData event = mock(RemoteEventData.class, "remoteEvent");

        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(this.changeRequestStorageManager.load(changeRequestId)).thenReturn(Optional.of(changeRequest));
        assertEquals(changeRequest, this.helper.getChangeRequest(changeRequestId, event));
        verify(this.changeRequestCacheManager).invalidate(changeRequestId);

        when(this.changeRequestStorageManager.load(changeRequestId)).thenReturn(Optional.empty());
        ChangeRequestEventsConverterException exception =
            assertThrows(ChangeRequestEventsConverterException.class,
                () -> this.helper.getChangeRequest(changeRequestId, event));
        assertEquals("Cannot find change request [someCRID] to convert event [remoteEvent]", exception.getMessage());

        ChangeRequestException crException = mock(ChangeRequestException.class);
        when(this.changeRequestStorageManager.load(changeRequestId)).thenThrow(crException);
        exception = assertThrows(ChangeRequestEventsConverterException.class,
                () -> this.helper.getChangeRequest(changeRequestId, event));
        assertEquals("Error when loading change request [someCRID] to convert event [remoteEvent]",
            exception.getMessage());
        assertEquals(crException, exception.getCause());
    }

    @Test
    void getFileChange() throws ChangeRequestException, ChangeRequestEventsConverterException
    {
        String changeRequestId = "someCR42";
        String fileChangeId = "someFilechange";
        RemoteEventData event = mock(RemoteEventData.class, "remoteEvent");
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(this.changeRequestStorageManager.load(changeRequestId)).thenReturn(Optional.of(changeRequest));
        FileChange fileChange = mock(FileChange.class);
        when(changeRequest.getFileChangeById(fileChangeId)).thenReturn(Optional.of(fileChange));

        assertEquals(fileChange, this.helper.getFileChange(changeRequestId, fileChangeId, event));

        when(changeRequest.getFileChangeById(fileChangeId)).thenReturn(Optional.empty());
        ChangeRequestEventsConverterException exception = assertThrows(ChangeRequestEventsConverterException.class,
                () -> this.helper.getFileChange(changeRequestId, fileChangeId, event));
        assertEquals("Cannot find file change [someFilechange] from change request [someCR42] to convert "
                + "event [remoteEvent].", exception.getMessage());

        when(this.changeRequestStorageManager.load(changeRequestId)).thenReturn(Optional.empty());
        exception = assertThrows(ChangeRequestEventsConverterException.class,
            () -> this.helper.getFileChange(changeRequestId, fileChangeId, event));
        assertEquals("Cannot find change request [someCR42] to convert event [remoteEvent]", exception.getMessage());
    }
}
