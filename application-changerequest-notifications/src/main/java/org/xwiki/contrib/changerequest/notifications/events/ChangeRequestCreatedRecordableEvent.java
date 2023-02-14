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

import org.xwiki.stability.Unstable;

/**
 * Recordable event of a created change request to send notification about this.
 *
 * @version $Id$
 * @since 0.1
 */
@Unstable
public class ChangeRequestCreatedRecordableEvent extends AbstractChangeRequestRecordableEvent
{
    /**
     * Event name to be used in the components referring to that event.
     */
    public static final String EVENT_NAME = "changerequest.create";

    private boolean fromSplit;
    private final String fileChangeId;

    /**
     * Default empty constructor.
     */
    public ChangeRequestCreatedRecordableEvent()
    {
        this(null, null);
    }

    /**
     * Default constructor with a change request id.
     * @param id the identifier of a change request for which the event is triggered.
     * @param fileChangeId the identifier of the filechange created.
     */
    public ChangeRequestCreatedRecordableEvent(String id, String fileChangeId)
    {
        super(id);
        this.fromSplit = false;
        this.fileChangeId = fileChangeId;
    }

    /**
     * @return {@code true} if the change request is created because of a split.
     */
    public boolean isFromSplit()
    {
        return fromSplit;
    }

    /**
     * @param fromSplit set {@code true} if the change request is created because of a split.
     */
    public void setFromSplit(boolean fromSplit)
    {
        this.fromSplit = fromSplit;
    }

    /**
     * @return the identifier of the file change added leading to this event.
     */
    public String getFileChangeId()
    {
        return fileChangeId;
    }

    @Override
    public String getEventName()
    {
        return EVENT_NAME;
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        return otherEvent instanceof ChangeRequestCreatedRecordableEvent;
    }
}
