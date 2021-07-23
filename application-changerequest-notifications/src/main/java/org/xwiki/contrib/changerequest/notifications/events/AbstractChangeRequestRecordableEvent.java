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

import org.xwiki.eventstream.RecordableEvent;
import org.xwiki.stability.Unstable;

/**
 * Abstract class for the various change request events.
 *
 * @version $Id$
 * @since 0.3
 */
@Unstable
public abstract class AbstractChangeRequestRecordableEvent implements RecordableEvent
{
    private final String changeRequestId;

    /**
     * Default constructor with a change request id.
     * @param id the identifier of a change request for which the event is triggered.
     */
    public AbstractChangeRequestRecordableEvent(String id)
    {
        this.changeRequestId = id;
    }

    /**
     * @return the identifier of a change request for which the event is triggered.
     */
    public String getChangeRequestId()
    {
        return changeRequestId;
    }

    /**
     * @return an event name that match the templates.
     */
    public abstract String getEventName();
}
