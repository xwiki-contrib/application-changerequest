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
package org.xwiki.contrib.changerequest.replication.internal.listeners;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.slf4j.Logger;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.changerequest.replication.internal.messages.AbstractChangeRequestEventReplicationSenderMessage;
import org.xwiki.contrib.changerequest.replication.internal.messages.ChangeRequestReplicationSenderMessage;
import org.xwiki.contrib.replication.ReplicationContext;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationSender;
import org.xwiki.contrib.replication.entity.DocumentReplicationController;
import org.xwiki.contrib.replication.entity.DocumentReplicationControllerInstance;
import org.xwiki.eventstream.RecordableEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.RemoteObservationManagerContext;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Abstract listener that aims at creating and sending the messages for replication of change request events.
 *
 * @param <T> the type of {@link RecordableEvent} that needs to be sent (note: it's mainly used for
 *            {@link org.xwiki.contrib.changerequest.notifications.events.AbstractChangeRequestRecordableEvent} but we
 *            have few events not using that abstraction.)
 * @param <R> the type of message that should be created and sent.
 *
 * @version $Id$
 * @since 0.16
 */
public abstract class AbstractChangeRequestEventListener<T extends RecordableEvent,
    R extends AbstractChangeRequestEventReplicationSenderMessage<T>> extends AbstractEventListener
{
    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Inject
    private Provider<ReplicationSender> replicationSenderProvider;

    @Inject
    private Provider<DocumentReplicationController> replicationControllerProvider;

    @Inject
    private RemoteObservationManagerContext remoteObservationManagerContext;

    @Inject
    private ReplicationContext replicationContext;

    @Inject
    private Logger logger;

    /**
     * Default constructor.
     *
     * @param name the name of the listener.
     * @param event the event to handle.
     */
    AbstractChangeRequestEventListener(String name, T event)
    {
        super(name, event);
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (!this.remoteObservationManagerContext.isRemoteState() && !this.replicationContext.isReplicationMessage()) {
            String messageHint = getMessageHint();
            XWikiDocument dataDoc = (XWikiDocument) data;
            this.processMessage((T) event, messageHint, dataDoc.getDocumentReference());
        }
    }

    private List<ReplicationInstance> getInstances(DocumentReference documentReference)
    {
        List<ReplicationInstance> result = Collections.emptyList();
        try {
            List<DocumentReplicationControllerInstance> instances =
                this.replicationControllerProvider.get().getReplicationConfiguration(documentReference);
            result = instances.stream()
                .map(DocumentReplicationControllerInstance::getInstance)
                .collect(Collectors.toList());
        } catch (ReplicationException e) {
            this.logger.error("Error while getting replication instances for document reference [{}]",
                documentReference, e);
        }
        return result;
    }

    /**
     * Retrieve the replication instances related to the given document reference, and send the message to them.
     *
     * @param message the message to be sent.
     * @param dataDocumentReference the reference of the document from which to retrieve the replication instances.
     */
    protected void sendMessage(R message, DocumentReference dataDocumentReference)
    {
        List<ReplicationInstance> instances = this.getInstances(dataDocumentReference);
        if (!instances.isEmpty()) {
            try {
                this.replicationSenderProvider.get().send(message, instances);
            } catch (ReplicationException e) {
                this.logger.error("Error while sending the replication message [{}] for document [{}]", message,
                    dataDocumentReference, e);
            }
        }
    }

    /**
     * Create a new instance of a message associated to the event, initialize it with event information and finally
     * sent it to the replicated instances.
     *
     * @param event the event from which to create the message.
     * @param messageHint the hint of the {@link ChangeRequestReplicationSenderMessage}: by convention it should be the
     *                    name of the event.
     * @param dataDocumentReference the reference of the document sent along with the event, used for initializing the
     *                              message, and for retrieving the replication instances.
     */
    protected void processMessage(T event, String messageHint, DocumentReference dataDocumentReference)
    {
        try {
            R message = this.componentManager.getInstance(ChangeRequestReplicationSenderMessage.class, messageHint);
            message.initialize(event, dataDocumentReference);
            this.sendMessage(message, dataDocumentReference);
        } catch (ComponentLookupException e) {
            this.logger.error("Error when looking for replication component message", e);
        }
    }

    /**
     * The hint of the {@link ChangeRequestReplicationSenderMessage} component to be used in
     * {@link #processMessage(RecordableEvent, String, DocumentReference)}. By convention this hint is generally
     * the name of the event received.
     *
     * @return the hint to be used for loading the {@link ChangeRequestReplicationSenderMessage} component.
     */
    public abstract String getMessageHint();
}
