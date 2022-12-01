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

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.changerequest.replication.internal.messages.ChangeRequestReplicationSenderMessage;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationInstanceManager;
import org.xwiki.contrib.replication.ReplicationInstanceRecoverHandler;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.contrib.replication.ReplicationSender;
import org.xwiki.contrib.replication.log.ReplicationMessageEventQuery;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventSearchResult;
import org.xwiki.eventstream.EventStore;
import org.xwiki.eventstream.query.SortableEventQuery;

/**
 * Generic abstract implementation of {@link ReplicationInstanceRecoverHandler} for change request.
 * This implementation will search for the events with the proper type, create the messages and intialize them based on
 * {@link ChangeRequestReplicationSenderMessage#initializeFromEventStream(Event)} and then send it back to the instance
 * requesting for recovery. The only thing that concrete implementation needs to provide is the hint of the messages
 * that is used both for finding the events, and for finding the component to initialize the messages.
 *
 * @version $Id$
 * @since 1.4
 */
public abstract class AbstractChangeRequestReplicationRecoverHandler implements ReplicationInstanceRecoverHandler
{
    @Inject
    private ReplicationInstanceManager instances;

    @Inject
    private ReplicationSender replicationSender;

    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Inject
    private EventStore eventStore;

    @Inject
    private Logger logger;

    @Override
    public void receive(Date dateMin, Date dateMax, ReplicationReceiverMessage message)
        throws ReplicationException
    {
        ReplicationInstance sourceInstance = this.instances.getInstanceByURI(message.getSource());

        // Taking care of this only in direct linked instances is enough
        if (sourceInstance == null) {
            return;
        }

        ReplicationMessageEventQuery query = new ReplicationMessageEventQuery();

        // Get all message related to likes
        query.eq(Event.FIELD_TYPE,
            ReplicationMessageEventQuery.messageTypeValue(getHint()));

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

        // Search with only the needed field in the result
        try (EventSearchResult result = this.eventStore.search(query)) {
            result.stream().forEach(event ->
                this.processMessage(event, sourceInstance));
        } catch (Exception e) {
            throw new ReplicationException("Failed to request messages log", e);
        }
    }

    private void processMessage(Event event, ReplicationInstance source)
    {
        ChangeRequestReplicationSenderMessage message = null;
        try {
            message =
                this.componentManager.getInstance(ChangeRequestReplicationSenderMessage.class, getHint());
            message.initializeFromEventStream(event);
            this.replicationSender.send(message, List.of(source));
        } catch (ComponentLookupException e) {
            this.logger.error("Error when looking for replication component message", e);
        } catch (ReplicationException e) {
            this.logger.error("Error while sending the replication message [{}] for instance [{}]", message,
                source, e);
        }
    }

    /**
     * @return the type of the messages
     */
    protected abstract String getHint();
}
