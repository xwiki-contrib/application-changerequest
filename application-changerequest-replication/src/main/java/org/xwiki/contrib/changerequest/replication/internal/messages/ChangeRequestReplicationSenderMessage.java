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

import org.xwiki.component.annotation.Role;
import org.xwiki.contrib.replication.ReplicationSenderMessage;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.RecordableEvent;
import org.xwiki.model.reference.DocumentReference;

/**
 * Default interface representing a {@link ReplicationSenderMessage} in change request.
 * This is defined as a component role since we use component injection for serializing some properties.
 * Note that the component implementations should all use
 * {@link org.xwiki.component.descriptor.ComponentInstantiationStrategy#PER_LOOKUP}.
 *
 * @version $Id$
 * @since 0.16
 */
@Role
public interface ChangeRequestReplicationSenderMessage extends ReplicationSenderMessage
{
    /**
     * Initialize the sender message with the different properties contained in the event.
     * The sender message will also contain the information of the given data document triggered with the event.
     * Once initialized the sender message should be ready to be sent to replicated instances.
     *
     * @param event the event that might contain property to put in the sender message.
     * @param dataDocumentReference the document reference to save in the sender message.
     */
    void initialize(RecordableEvent event, DocumentReference dataDocumentReference);

    /**
     * Initialize the sender message with the different properties contained in the event.
     *
     * @param event the event already stored that needs to be replicated
     */
    void initializeFromEventStream(Event event);
}
