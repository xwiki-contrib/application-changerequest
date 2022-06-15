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
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestDiscussionRecordableEvent;

/**
 * Default sender message implementation for {@link ChangeRequestDiscussionRecordableEvent}.
 *
 * @version $Id$
 * @since 0.16
 */
@Component
@Named(ChangeRequestDiscussionRecordableEvent.EVENT_NAME)
public class ChangeRequestDiscussionReplicationSenderMessage
    extends AbstractRecordableChangeRequestEventReplicationSenderMessage<ChangeRequestDiscussionRecordableEvent>
{
    /**
     * Key of the custom metadata holding the discussion type information.
     * @see ChangeRequestDiscussionRecordableEvent#getDiscussionType()
     */
    public static final String DISCUSSION_TYPE = "DISCUSSION_TYPE";

    /**
     * Key of the custom metadata holding the discussion reference information.
     * @see ChangeRequestDiscussionRecordableEvent#getDiscussionReference()
     */
    public static final String DISCUSSION_REFERENCE = "DISCUSSION_REFERENCE";

    /**
     * Key of the custom metadata holding the message reference information.
     * @see ChangeRequestDiscussionRecordableEvent#getMessageReference()
     */
    public static final String MESSAGE_REFERENCE = "MESSAGE_REFERENCE";

    /**
     * Default constructor.
     */
    public ChangeRequestDiscussionReplicationSenderMessage()
    {
        super(ChangeRequestDiscussionRecordableEvent.EVENT_NAME);
    }

    @Override
    protected void initializeCustomMetadata(ChangeRequestDiscussionRecordableEvent event)
    {
        this.putCustomMetadata(DISCUSSION_TYPE, event.getDiscussionType());
        this.putCustomMetadata(DISCUSSION_REFERENCE, event.getDiscussionReference());
        this.putCustomMetadata(MESSAGE_REFERENCE, event.getMessageReference());
    }
}
