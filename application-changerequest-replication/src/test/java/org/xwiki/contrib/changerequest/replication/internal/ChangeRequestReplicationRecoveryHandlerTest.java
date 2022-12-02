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
package org.xwiki.contrib.changerequest.replication.internal;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestCreatedRecordableEvent;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestDiscussionRecordableEvent;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestFileChangeAddedRecordableEvent;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestReadyForReviewTargetableEvent;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestRebasedRecordableEvent;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestReviewAddedRecordableEvent;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestStatusChangedRecordableEvent;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestUpdatedRecordableEvent;
import org.xwiki.contrib.changerequest.notifications.events.DocumentModifiedInChangeRequestEvent;
import org.xwiki.contrib.changerequest.notifications.events.StaleChangeRequestRecordableEvent;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.contrib.replication.ReplicationSender;
import org.xwiki.contrib.replication.log.ReplicationMessageEventQuery;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.query.SortableEventQuery;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ChangeRequestReplicationRecoveryHandler}.
 *
 * @version $Id$
 */
@ComponentTest
class ChangeRequestReplicationRecoveryHandlerTest
{
    @InjectMockComponents
    private ChangeRequestReplicationRecoveryHandler requestReplicationRecoveryHandler;

    @MockComponent
    private ReplicationSender replicationSender;

    @Test
    void receive() throws ReplicationException
    {
        Date dateMin = new Date(41);
        Date dateMax = new Date(4444);

        ReplicationMessageEventQuery query = new ReplicationMessageEventQuery();

        // Get all events related to Change request
        query.in(Event.FIELD_TYPE, List.of(
            ReplicationMessageEventQuery.messageTypeValue(ChangeRequestCreatedRecordableEvent.EVENT_NAME),
            ReplicationMessageEventQuery.messageTypeValue(ChangeRequestDiscussionRecordableEvent.EVENT_NAME),
            ReplicationMessageEventQuery.messageTypeValue(ChangeRequestRebasedRecordableEvent.EVENT_NAME),
            ReplicationMessageEventQuery.messageTypeValue(ChangeRequestUpdatedRecordableEvent.EVENT_NAME),
            ReplicationMessageEventQuery.messageTypeValue(ChangeRequestFileChangeAddedRecordableEvent.EVENT_NAME),
            ReplicationMessageEventQuery.messageTypeValue(ChangeRequestReadyForReviewTargetableEvent.EVENT_NAME),
            ReplicationMessageEventQuery.messageTypeValue(ChangeRequestReviewAddedRecordableEvent.EVENT_NAME),
            ReplicationMessageEventQuery.messageTypeValue(ChangeRequestStatusChangedRecordableEvent.EVENT_NAME),
            ReplicationMessageEventQuery.messageTypeValue(StaleChangeRequestRecordableEvent.EVENT_NAME),
            ReplicationMessageEventQuery.messageTypeValue(DocumentModifiedInChangeRequestEvent.EVENT_NAME)
        ));

        // And only the stored and received ones
        query.custom().in(
            ReplicationMessageEventQuery.KEY_STATUS,
            ReplicationMessageEventQuery.VALUE_STATUS_STORED,
            ReplicationMessageEventQuery.VALUE_STATUS_RECEIVED);

        // Minimum date
        query.after(dateMin);
        query.before(dateMax);
        // Sort by date
        query.addSort(Event.FIELD_DATE, SortableEventQuery.SortClause.Order.ASC);

        ReplicationReceiverMessage message = mock(ReplicationReceiverMessage.class);
        when(message.getSource()).thenReturn("toto");

        this.requestReplicationRecoveryHandler.receive(dateMin, dateMax, message);
        verify(this.replicationSender).resend(query, List.of(message.getSource()));
    }
}