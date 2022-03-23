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
 * Event triggered when a new discussion message is created.
 *
 * @version $Id$
 * @since 0.6
 */
@Unstable
public class ChangeRequestDiscussionRecordableEvent extends AbstractChangeRequestRecordableEvent
{
    /**
     * Event name to be used in the components referring to that event.
     */
    public static final String EVENT_NAME = "changerequest.discussions";

    private final String discussionType;

    private final String discussionReference;

    private final String messageReference;

    /**
     * Default empty constructor.
     */
    public ChangeRequestDiscussionRecordableEvent()
    {
        this(null, null, null, null);
    }

    /**
     * Default constructor with a change request id.
     * @param id the identifier of a change request for which the event is triggered.
     * @param discussionType the type of the discussion
     * @param discussionReference the reference of the discussion
     * @param messageReference the reference of the message
     * @see org.xwiki.contrib.changerequest.discussions.references.AbstractChangeRequestDiscussionContextReference
     */
    public ChangeRequestDiscussionRecordableEvent(String id, String discussionType, String discussionReference,
        String messageReference)
    {
        super(id);
        this.discussionType = discussionType;
        this.discussionReference = discussionReference;
        this.messageReference = messageReference;
    }

    /**
     * @return the type of the discussion
     */
    public String getDiscussionType()
    {
        return discussionType;
    }

    /**
     * @return the reference of the discussion
     */
    public String getDiscussionReference()
    {
        return discussionReference;
    }

    /**
     * @return the reference of the message
     */
    public String getMessageReference()
    {
        return this.messageReference;
    }

    @Override
    public String getEventName()
    {
        return EVENT_NAME;
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        return otherEvent instanceof ChangeRequestDiscussionRecordableEvent;
    }
}
