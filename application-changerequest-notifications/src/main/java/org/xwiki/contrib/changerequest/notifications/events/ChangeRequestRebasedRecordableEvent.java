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

import org.apache.commons.lang3.StringUtils;
import org.xwiki.stability.Unstable;

/**
 * Recordable event of a rebase action in a whole change request, or in a specific filechange.
 * Note that in case of a conflict fix, this event is also triggered.
 *
 * @version $Id$
 * @since 0.10
 */
@Unstable
public class ChangeRequestRebasedRecordableEvent extends AbstractChangeRequestRecordableEvent
{
    /**
     * Event name to be used in the components referring to that event.
     */
    public static final String EVENT_NAME = "changerequest.rebased";

    private final boolean allChangeRequest;
    private final boolean isConflictFixing;
    private final String fileChangeId;

    /**
     * Default empty constructor.
     */
    public ChangeRequestRebasedRecordableEvent()
    {
        this(null, false, null);
    }

    /**
     * Default constructor.
     *
     * @param id the id of the changerequest.
     * @param isConflictFixing {@code true} if the event is triggered after a fix conflict.
     * @param fileChangeId {@code null} if it concerns the whole change request, else the id of the concerned
     *                     filechange.
     */
    public ChangeRequestRebasedRecordableEvent(String id, boolean isConflictFixing, String fileChangeId)
    {
        super(id);
        this.fileChangeId = fileChangeId;
        this.allChangeRequest = StringUtils.isEmpty(fileChangeId);
        this.isConflictFixing = isConflictFixing;
    }

    @Override
    public String getEventName()
    {
        return EVENT_NAME;
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        return otherEvent instanceof ChangeRequestRebasedRecordableEvent;
    }

    /**
     * @return {@code true} if the event concerns a rebase of the whole change request.
     */
    public boolean concernsAllChangeRequest()
    {
        return this.allChangeRequest;
    }

    /**
     * @return the identifier of the concerned filechange.
     */
    public String getConcernedFileChangeId()
    {
        return this.fileChangeId;
    }

    /**
     * @return {@code true} if the event is related to a conflict fixing.
     */
    public boolean isConflictFixing()
    {
        return this.isConflictFixing;
    }
}
