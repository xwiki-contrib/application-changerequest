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
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.notifications.events.AbstractChangeRequestRecordableEvent;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationSender;
import org.xwiki.contrib.replication.ReplicationSenderMessage;
import org.xwiki.contrib.replication.entity.DocumentReplicationController;
import org.xwiki.contrib.replication.entity.DocumentReplicationControllerInstance;
import org.xwiki.eventstream.RecordableEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;

/**
 * Helper component for sending messages.
 *
 * @version $Id$
 * @since 1.4
 */
@Component(roles = ChangeRequestReplicationMessageSender.class)
@Singleton
public class ChangeRequestReplicationMessageSender
{
    @Inject
    private Provider<ReplicationSender> replicationSenderProvider;

    @Inject
    private Provider<DocumentReplicationController> replicationControllerProvider;

    @Inject
    private Provider<ChangeRequestStorageManager> changeRequestStorageManagerProvider;

    @Inject
    private DocumentReferenceResolver<ChangeRequest> changeRequestDocumentReferenceResolver;

    @Inject
    private Logger logger;

    private List<ReplicationInstance> getInstances(String changeRequestId)
    {
        List<ReplicationInstance> result = Collections.emptyList();
        try {
            Optional<ChangeRequest> optionalChangeRequest
                = this.changeRequestStorageManagerProvider.get().load(changeRequestId);
            if (optionalChangeRequest.isPresent()) {
                DocumentReference documentReference =
                    this.changeRequestDocumentReferenceResolver.resolve(optionalChangeRequest.get());
                result = this.getInstances(documentReference);
            } else {
                this.logger.error("No change request found with identifier [{}]", changeRequestId);
            }
        } catch (ChangeRequestException e) {
            this.logger.error("Cannot load change request [{}]", changeRequestId, e);
        }
        return result;
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
     * @param event the event triggering the message
     */
    public void sendMessage(ReplicationSenderMessage message, DocumentReference dataDocumentReference,
        RecordableEvent event)
    {
        List<ReplicationInstance> instances;
        if (event instanceof AbstractChangeRequestRecordableEvent) {
            instances = this.getInstances(((AbstractChangeRequestRecordableEvent) event).getChangeRequestId());
        } else {
            instances = this.getInstances(dataDocumentReference);
        }
        if (!instances.isEmpty()) {
            try {
                this.replicationSenderProvider.get().send(message, instances);
            } catch (ReplicationException e) {
                this.logger.error("Error while sending the replication message [{}] for document [{}]", message,
                    dataDocumentReference, e);
            }
        }
    }
}
