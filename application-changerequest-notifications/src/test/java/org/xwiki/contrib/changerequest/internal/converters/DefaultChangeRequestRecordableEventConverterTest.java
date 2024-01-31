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
import org.xwiki.contrib.changerequest.notifications.events.DocumentModifiedInChangeRequestEvent;
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
 * Tests for {@link DefaultChangeRequestRecordableEventConverter}.
 *
 * @version $Id$
 * @since 0.11
 */

@ComponentTest
class DefaultChangeRequestRecordableEventConverterTest
{
    @InjectMockComponents
    private DefaultChangeRequestRecordableEventConverter converter;

    @MockComponent
    private RecordableEventConverter defaultConverter;

    @Test
    void convertChangeRequestCreatedRecordableEvent() throws Exception
    {
        ChangeRequestCreatedRecordableEvent recordableEvent = mock(ChangeRequestCreatedRecordableEvent.class);
        Event event = mock(Event.class);
        String source = "a source";
        int data = 42;
        when(this.defaultConverter.convert(recordableEvent, source, data)).thenReturn(event);

        String crId = "myCr42";
        String eventName = "ChangeRequestCreatedRecordableEvent";
        when(recordableEvent.getChangeRequestId()).thenReturn(crId);
        when(recordableEvent.getEventName()).thenReturn(eventName);

        Map<String, Object> originalEventParameters = Collections.singletonMap("Foo", "42");
        when(event.getCustom()).thenReturn(originalEventParameters);

        Map<String, Object> expectedParameters = new HashMap<>(originalEventParameters);
        expectedParameters.put(AbstractChangeRequestRecordableEventConverter.CHANGE_REQUEST_ID_PARAMETER_KEY, crId);

        assertSame(event, this.converter.convert(recordableEvent, source, data));
        verify(event).setType(eventName);
        verify(event).setGroupId(eventName);
        verify(event).setCustom(expectedParameters);
    }

    @Test
    void convertDocumentModifiedInChangeRequestEvent() throws Exception
    {
        DocumentModifiedInChangeRequestEvent recordableEvent = mock(DocumentModifiedInChangeRequestEvent.class);
        Event event = mock(Event.class);
        String source = "a source";
        int data = 42;
        when(this.defaultConverter.convert(recordableEvent, source, data)).thenReturn(event);

        String crId = "myCr42";
        String eventName = "DocumentModifiedInChangeRequestEvent";
        when(recordableEvent.getChangeRequestId()).thenReturn(crId);
        when(recordableEvent.getEventName()).thenReturn(eventName);

        Map<String, Object> originalEventParameters = Collections.singletonMap("Foo", "42");
        when(event.getCustom()).thenReturn(originalEventParameters);

        Map<String, Object> expectedParameters = new HashMap<>(originalEventParameters);
        expectedParameters.put(AbstractChangeRequestRecordableEventConverter.CHANGE_REQUEST_ID_PARAMETER_KEY, crId);

        assertSame(event, this.converter.convert(recordableEvent, source, data));
        verify(event).setType(eventName);
        verify(event).setGroupId(eventName);
        verify(event).setCustom(expectedParameters);
    }
}
