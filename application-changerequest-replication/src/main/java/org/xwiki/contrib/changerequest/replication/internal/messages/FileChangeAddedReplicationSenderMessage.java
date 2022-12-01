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
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestFileChangeAddedRecordableEvent;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.RecordableEvent;

/**
 * Default sender message implementation for {@link ChangeRequestFileChangeAddedRecordableEvent}.
 *
 * @version $Id$
 * @since 0.16
 */
@Component
@Named(ChangeRequestFileChangeAddedRecordableEvent.EVENT_NAME)
public class FileChangeAddedReplicationSenderMessage
    extends AbstractRecordableChangeRequestEventReplicationSenderMessage
{
    /**
     * Key of the custom metadata holding the filechange identifier.
     * @see ChangeRequestFileChangeAddedRecordableEvent#getFileChangeId()
     */
    public static final String FILE_CHANGE_ID = "FILE_CHANGE_ID";

    /**
     * Default constructor.
     */
    public FileChangeAddedReplicationSenderMessage()
    {
        super(ChangeRequestFileChangeAddedRecordableEvent.EVENT_NAME);
    }

    @Override
    protected void initializeCustomMetadata(RecordableEvent event)
    {
        this.putCustomMetadata(FILE_CHANGE_ID, ((ChangeRequestFileChangeAddedRecordableEvent) event).getFileChangeId());
    }

    @Override
    public void initializeCustomMetadata(Event event)
    {
        this.putCustomMetadata(FILE_CHANGE_ID, event.getCustom().get(FILE_CHANGE_ID));
    }
}
