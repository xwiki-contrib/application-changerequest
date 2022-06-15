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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Provider;

import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.contrib.changerequest.notifications.events.AbstractChangeRequestRecordableEvent;
import org.xwiki.contrib.replication.AbstractReplicationMessage;
import org.xwiki.eventstream.RecordableEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import com.xpn.xwiki.XWikiContext;

/**
 * Abstract implementation of a replication sender message for change request.
 * This implementation is written to work for any {@link RecordableEvent} but was mainly meant to be used with a
 * {@link AbstractChangeRequestRecordableEvent}: see
 * {@link AbstractRecordableChangeRequestEventReplicationSenderMessage}.
 * Note that this abstract component already defines the instantiation strategy as
 * {@link ComponentInstantiationStrategy#PER_LOOKUP}.
 *
 * Initializing a message from an event will automatically put some custom metadata: by default, it stores the current
 * context user reference, and the reference of the document triggered along with the event. Now the
 * {@link #initialize(RecordableEvent, DocumentReference)} method also calls the abstract method
 * {@link #initializeCustomMetadata(RecordableEvent)} which aims at storing the custom metadata for each type of event.
 *
 * @param <T> the type of {@link RecordableEvent} the component should be used with.
 *
 * @version $Id$
 * @since 0.16
 */
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public abstract class AbstractChangeRequestEventReplicationSenderMessage<T extends RecordableEvent>
    extends AbstractReplicationMessage
    implements ChangeRequestReplicationSenderMessage<T>
{
    /**
     * Key of the custom metadata for holding the reference of the document triggered along with the event.
     */
    public static final String DATA_DOCUMENT = "DATA_DOCUMENT";

    /**
     * Key of the custom metadata for holding the reference of the context user at the moment when the event was
     * triggered.
     */
    public static final String CONTEXT_USER = "CONTEXT_USER";

    @Inject
    protected EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private Provider<XWikiContext> contextProvider;

    private final String id;
    private final Date date;
    private final String type;

    /**
     * Default constructor.
     *
     * @param type the type of message: by convention we should use the event name.
     */
    AbstractChangeRequestEventReplicationSenderMessage(String type)
    {
        this.id = UUID.randomUUID().toString();
        this.date = new Date();
        this.type = type;
    }

    @Override
    public String getId()
    {
        return this.id;
    }

    @Override
    public Date getDate()
    {
        return this.date;
    }

    @Override
    public String getSource()
    {
        return null;
    }

    @Override
    public String getType()
    {
        return this.type;
    }

    private void saveContextUser()
    {
        String serializedUser = this.entityReferenceSerializer.serialize(contextProvider.get().getUserReference());
        this.putCustomMetadata(CONTEXT_USER, serializedUser);
    }

    @Override
    public void write(OutputStream stream) throws IOException
    {
        // No content associated with this event
    }

    @Override
    public void initialize(T event, DocumentReference dataDocument)
    {
        this.saveContextUser();
        this.putCustomMetadata(DATA_DOCUMENT, this.entityReferenceSerializer.serialize(dataDocument));
        this.initializeCustomMetadata(event);
    }

    /**
     * Initialize the message with the custom data provided by the event.
     *
     * @param event the event from which to retrieve the custom data to be stored in the message.
     */
    protected abstract void initializeCustomMetadata(T event);
}
