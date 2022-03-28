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
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestRebasedRecordableEvent;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.RecordableEventConverter;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ChangeRequestRebasedRecordableEventConverter}.
 *
 * @version $Id$
 * @since 0.11
 */

@ComponentTest
class ChangeRequestRebasedRecordableEventConverterTest
{
    @InjectMockComponents
    private ChangeRequestRebasedRecordableEventConverter converter;

    @MockComponent
    private RecordableEventConverter defaultConverter;

    @Test
    void convert() throws Exception
    {
        ChangeRequestRebasedRecordableEvent recordableEvent = mock(ChangeRequestRebasedRecordableEvent.class);
        Event event = mock(Event.class);
        String source = "a source";
        int data = 42;
        when(this.defaultConverter.convert(recordableEvent, source, data)).thenReturn(event);

        String crId = "myCr42";
        String eventName = "ChangeRequestRebasedRecordableEvent";
        when(recordableEvent.getChangeRequestId()).thenReturn(crId);
        when(recordableEvent.getEventName()).thenReturn(eventName);

        Map<String, String> originalEventParameters = Collections.singletonMap("Foo", "42");
        when(event.getParameters()).thenReturn(originalEventParameters);

        Map<String, String> expectedParameters = new HashMap<>(originalEventParameters);
        expectedParameters.put(ChangeRequestRebasedRecordableEventConverter.CHANGE_REQUEST_ID_PARAMETER_KEY, crId);

        String fileChangeID = "fileChange4335";
        when(recordableEvent.concernsAllChangeRequest()).thenReturn(true);
        when(recordableEvent.isConflictFixing()).thenReturn(false);
        when(recordableEvent.getConcernedFileChangeId()).thenReturn(fileChangeID);
        expectedParameters.put(ChangeRequestRebasedRecordableEventConverter.REBASED_FILECHANGE_KEY, fileChangeID);
        expectedParameters.put(ChangeRequestRebasedRecordableEventConverter.REBASED_ALL_KEY, "true");
        expectedParameters.put(ChangeRequestRebasedRecordableEventConverter.REBASED_WITH_CONFLICT_FIXING, "false");

        assertSame(event, this.converter.convert(recordableEvent, source, data));
        verify(event).setType(eventName);
        verify(event).setGroupId(eventName);
        verify(event).setParameters(expectedParameters);
    }
}
