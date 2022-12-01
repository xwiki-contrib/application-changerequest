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
package org.xwiki.contrib.changerequest.replication.internal.messages;

import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestStatusChangedRecordableEvent;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.RecordableEvent;

/**
 * Default sender message implementation for {@link ChangeRequestStatusChangedRecordableEvent}.
 *
 * @version $Id$
 * @since 0.16
 */
@Component
@Named(ChangeRequestStatusChangedRecordableEvent.EVENT_NAME)
public class StatusUpdatedReplicationSenderMessage extends AbstractRecordableChangeRequestEventReplicationSenderMessage
{
    /**
     * Key of the custom metadata holding the new status before the event.
     * @see ChangeRequestStatusChangedRecordableEvent#getNewStatus()
     */
    public static final String NEW_STATUS = "NEW_STATUS";

    /**
     * Key of the custom metadata holding the old status before the event.
     * @see ChangeRequestStatusChangedRecordableEvent#getOldStatus()
     */
    public static final String OLD_STATUS = "OLD_STATUS";

    /**
     * Default constructor.
     */
    public StatusUpdatedReplicationSenderMessage()
    {
        super(ChangeRequestStatusChangedRecordableEvent.EVENT_NAME);
    }

    @Override
    protected void initializeCustomMetadata(RecordableEvent recordableEvent)
    {
        ChangeRequestStatusChangedRecordableEvent event = (ChangeRequestStatusChangedRecordableEvent) recordableEvent;
        this.putCustomMetadata(NEW_STATUS, event.getNewStatus().name());
        this.putCustomMetadata(OLD_STATUS, event.getOldStatus().name());
    }

    @Override
    public void initializeCustomMetadata(Event event)
    {
        this.putCustomMetadata(NEW_STATUS, event.getCustom().get(NEW_STATUS));
        this.putCustomMetadata(OLD_STATUS, event.getCustom().get(OLD_STATUS));
    }
}
