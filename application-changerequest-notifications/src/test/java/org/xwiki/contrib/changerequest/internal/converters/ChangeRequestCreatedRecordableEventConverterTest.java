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
package org.xwiki.contrib.changerequest.internal.converters;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestCreatedRecordableEvent;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.RecordableEventConverter;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
class ChangeRequestCreatedRecordableEventConverterTest
{
    @InjectMockComponents
    private ChangeRequestCreatedRecordableEventConverter converter;

    @MockComponent
    private RecordableEventConverter defaultConverter;

    @Test
    void convert() throws Exception
    {
        ChangeRequestCreatedRecordableEvent recordableEvent =
            mock(ChangeRequestCreatedRecordableEvent.class);
        Event event = mock(Event.class);
        String source = "a source";
        int data = 42;
        when(this.defaultConverter.convert(recordableEvent, source, data)).thenReturn(event);

        String crId = "myCr42";
        String fileChangeId = "someFileChangeId";
        String eventName = "ChangeRequestFileChangeAddedRecordableEvent";
        when(recordableEvent.getChangeRequestId()).thenReturn(crId);
        when(recordableEvent.getEventName()).thenReturn(eventName);

        Map<String, String> originalEventParameters = Collections.singletonMap("Foo", "42");
        when(event.getParameters()).thenReturn(originalEventParameters);

        Map<String, String> expectedParameters = new HashMap<>(originalEventParameters);
        expectedParameters.put(ChangeRequestCreatedRecordableEventConverter.CHANGE_REQUEST_ID_PARAMETER_KEY,
            crId);

        when(recordableEvent.isFromSplit()).thenReturn(true);
        when(recordableEvent.getFileChangeId()).thenReturn(fileChangeId);
        expectedParameters.put(ChangeRequestCreatedRecordableEventConverter.IS_FROM_SPLIT_PARAMETER, "true");
        expectedParameters.put(ChangeRequestFileChangeAddedRecordableEventConverter.FILECHANGE_ID_PARAMETER_KEY,
            fileChangeId);

        assertSame(event, this.converter.convert(recordableEvent, source, data));
        verify(event).setType(eventName);
        verify(event).setGroupId(eventName);
        verify(event).setParameters(expectedParameters);
    }
}
