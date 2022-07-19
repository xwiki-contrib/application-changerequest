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

import org.xwiki.contrib.changerequest.notifications.events.AbstractChangeRequestRecordableEvent;
import org.xwiki.eventstream.RecordableEvent;
import org.xwiki.model.reference.DocumentReference;

/**
 * Abstract component for defining a sender message from an {@link AbstractChangeRequestRecordableEvent}.
 * On top of the metadata that are automatically stored thanks to
 * {@link AbstractChangeRequestEventReplicationSenderMessage} the
 * {@link #initialize(RecordableEvent, DocumentReference)} method of this class also stores the
 * change request identifier of the event.
 *
 * @version $Id$
 * @since 0.16
 */
public abstract class AbstractRecordableChangeRequestEventReplicationSenderMessage extends
    AbstractChangeRequestEventReplicationSenderMessage
{
    /**
     * Key of the custom metadata holding the change request identifier of the event.
     */
    public static final String CHANGE_REQUEST_ID_PARAMETER = "CHANGE_REQUEST_ID";

    /**
     * Default constructor.
     *
     * @param type the type of the message: by convention it should be the name of the handled event.
     */
    AbstractRecordableChangeRequestEventReplicationSenderMessage(String type)
    {
        super(type);
    }

    @Override
    public void initialize(RecordableEvent event, DocumentReference dataDocument)
    {
        super.initialize(event, dataDocument);
        this.putCustomMetadata(CHANGE_REQUEST_ID_PARAMETER,
            ((AbstractChangeRequestRecordableEvent) event).getChangeRequestId());
    }
}
