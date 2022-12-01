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
package org.xwiki.contrib.changerequest.replication.internal.recovery;

import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.changerequest.events.ChangeRequestCreatedEvent;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestCreatedRecordableEvent;
import org.xwiki.contrib.changerequest.replication.internal.messages.ChangeRequestReplicationSenderMessage;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationInstanceManager;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.contrib.replication.ReplicationSender;
import org.xwiki.contrib.replication.log.ReplicationMessageEventQuery;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventSearchResult;
import org.xwiki.eventstream.EventStore;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.eventstream.query.SortableEventQuery;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ChangeRequestCreatedReplicationRecoverHandler}.
 *
 * @version $Id$
 */
@ComponentTest
class ChangeRequestCreatedReplicationRecoverHandlerTest
{
    @InjectMockComponents
    private ChangeRequestCreatedReplicationRecoverHandler recoverHandler;

    @MockComponent
    private ReplicationInstanceManager instances;

    @MockComponent
    private ReplicationSender replicationSender;

    @MockComponent
    @Named("context")
    private ComponentManager componentManager;

    @MockComponent
    private EventStore eventStore;

    @Test
    void receive() throws ReplicationException, EventStreamException, ComponentLookupException
    {
        ReplicationReceiverMessage originalMessage = mock(ReplicationReceiverMessage.class);
        Date dateMin = new Date(42);
        Date dateMax = new Date(4242);

        ReplicationInstance sourceInstance = mock(ReplicationInstance.class);
        String source = "sourceInstance";
        when(originalMessage.getSource()).thenReturn(source);
        when(this.instances.getInstanceByURI(source)).thenReturn(sourceInstance);

        ReplicationMessageEventQuery expectedQuery = new ReplicationMessageEventQuery();
        expectedQuery.eq(Event.FIELD_TYPE, ReplicationMessageEventQuery.messageTypeValue(
            ChangeRequestCreatedRecordableEvent.EVENT_NAME));
        expectedQuery.custom().in(
            ReplicationMessageEventQuery.KEY_STATUS,
            ReplicationMessageEventQuery.VALUE_STATUS_STORED,
            ReplicationMessageEventQuery.VALUE_STATUS_RECEIVED);
        expectedQuery.after(dateMin);
        expectedQuery.before(dateMax);
        expectedQuery.addSort(Event.FIELD_DATE, SortableEventQuery.SortClause.Order.ASC);

        Event event1 = mock(Event.class, "event1");
        Event event2 = mock(Event.class, "event2");
        Event event3 = mock(Event.class, "event3");
        Event event4 = mock(Event.class, "event4");

        EventSearchResult eventSearchResult = mock(EventSearchResult.class);
        when(this.eventStore.search(expectedQuery)).thenReturn(eventSearchResult);
        when(eventSearchResult.stream()).thenReturn(Stream.of(event1, event2, event3, event4));

        ChangeRequestReplicationSenderMessage senderMessage = mock(ChangeRequestReplicationSenderMessage.class);
        when(this.componentManager.getInstance(ChangeRequestReplicationSenderMessage.class,
            ChangeRequestCreatedRecordableEvent.EVENT_NAME)).thenReturn(senderMessage);

        this.recoverHandler.receive(dateMin, dateMax, originalMessage);
        verify(componentManager, times(4)).getInstance(ChangeRequestReplicationSenderMessage.class,
            ChangeRequestCreatedRecordableEvent.EVENT_NAME);
        verify(senderMessage).initializeFromEventStream(event1);
        verify(senderMessage).initializeFromEventStream(event2);
        verify(senderMessage).initializeFromEventStream(event3);
        verify(senderMessage).initializeFromEventStream(event4);
        verify(this.replicationSender, times(4)).send(senderMessage, List.of(sourceInstance));
    }
}