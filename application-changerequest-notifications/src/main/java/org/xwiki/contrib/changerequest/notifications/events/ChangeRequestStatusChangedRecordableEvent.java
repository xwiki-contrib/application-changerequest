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
package org.xwiki.contrib.changerequest.notifications.events;

import org.xwiki.contrib.changerequest.ChangeRequestStatus;
import org.xwiki.stability.Unstable;

/**
 * Event triggered when the status of the change request changes.
 *
 * @version $Id$
 * @since 0.6
 */
@Unstable
public class ChangeRequestStatusChangedRecordableEvent extends AbstractChangeRequestRecordableEvent
{
    /**
     * Event name to be used in the components referring to that event.
     */
    public static final String EVENT_NAME = "changerequest.status.modified";

    private ChangeRequestStatus oldStatus;
    private ChangeRequestStatus newStatus;

    /**
     * Default empty constructor.
     */
    public ChangeRequestStatusChangedRecordableEvent()
    {
        this(null, null, null);
    }

    /**
     * Default constructor with a change request id.
     *
     * @param id the identifier of a change request for which the event is triggered.
     * @param oldStatus the old status of the change request before the status change
     * @param newStatus the new status of the change request which led to this event.
     */
    public ChangeRequestStatusChangedRecordableEvent(String id, ChangeRequestStatus oldStatus,
        ChangeRequestStatus newStatus)
    {
        super(id);
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

    @Override
    public String getEventName()
    {
        return EVENT_NAME;
    }

    /**
     * @return the old status of the change request, before the change leading to this event.
     */
    public ChangeRequestStatus getOldStatus()
    {
        return oldStatus;
    }

    /**
     * @return the new status of the change request, after the change leading to this event.
     */
    public ChangeRequestStatus getNewStatus()
    {
        return newStatus;
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        return otherEvent instanceof ChangeRequestStatusChangedRecordableEvent;
    }
}
