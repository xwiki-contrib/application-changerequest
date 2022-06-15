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
package org.xwiki.contrib.changerequest.replication.internal.listeners;

import java.util.Collections;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestStatusChangedRecordableEvent;
import org.xwiki.contrib.changerequest.replication.internal.messages.StatusUpdatedReplicationSenderMessage;
import org.xwiki.observation.event.Event;

@Component
@Singleton
@Named(StatusUpdatedListener.NAME)
public class StatusUpdatedListener extends AbstractChangeRequestEventListener
{
    public static final String NAME =
        "org.xwiki.contrib.changerequest.replication.internal.listeners.StatusUpdatedListener";

    private static final List<Event> EVENT_LIST =
        Collections.singletonList(new ChangeRequestStatusChangedRecordableEvent());

    public StatusUpdatedListener()
    {
        super(NAME, EVENT_LIST);
    }

    @Override
    public void processLocalEvent(Event event, Object source, Object data)
    {
        ChangeRequestStatusChangedRecordableEvent statusChangeEvent = (ChangeRequestStatusChangedRecordableEvent) event;
        try {
            StatusUpdatedReplicationSenderMessage message =
                getComponentManager().getInstance(StatusUpdatedReplicationSenderMessage.class);
            message.initialize(statusChangeEvent);
            this.sendMessage(message, statusChangeEvent.getChangeRequestId());
        } catch (ComponentLookupException e) {
            this.logger.error("Error when loading the message component", e);
        }
    }
}
