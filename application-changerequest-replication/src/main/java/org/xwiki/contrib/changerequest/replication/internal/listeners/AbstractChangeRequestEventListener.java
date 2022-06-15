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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.slf4j.Logger;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.replication.internal.messages.AbstractChangeRequestEventReplicationSenderMessage;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.contrib.replication.ReplicationContext;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationInstance;
import org.xwiki.contrib.replication.ReplicationSender;
import org.xwiki.contrib.replication.entity.DocumentReplicationController;
import org.xwiki.contrib.replication.entity.DocumentReplicationControllerInstance;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.RemoteObservationManagerContext;

public abstract class AbstractChangeRequestEventListener extends AbstractEventListener
{
    public AbstractChangeRequestEventListener(String name, List<? extends Event> events)
    {
        super(name, events);
    }

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
    private Provider<ChangeRequestStorageManager> changeRequestStorageManagerProvider;

    @Inject
    private Provider<DocumentReferenceResolver<ChangeRequest>> documentReferenceResolverProvider;

    @Inject
    protected Logger logger;

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (!this.remoteObservationManagerContext.isRemoteState() && !this.replicationContext.isReplicationMessage()) {
            this.processLocalEvent(event, source, data);
        }
    }

    private List<ReplicationInstance> getInstances(String changeRequestId)
    {
        List<ReplicationInstance> result = Collections.emptyList();
        try {
            Optional<ChangeRequest> changeRequestOpt =
                this.changeRequestStorageManagerProvider.get().load(changeRequestId);
            if (changeRequestOpt.isPresent()) {
                ChangeRequest changeRequest = changeRequestOpt.get();
                DocumentReference documentReference =
                    this.documentReferenceResolverProvider.get().resolve(changeRequest);
                List<DocumentReplicationControllerInstance> instances =
                    this.replicationControllerProvider.get().getReplicationConfiguration(documentReference);
                result = instances.stream()
                    .map(DocumentReplicationControllerInstance::getInstance)
                    .collect(Collectors.toList());
            }
        } catch (ChangeRequestException e) {
            this.logger.error("Error while looking for change request [{}]", changeRequestId, e);
        } catch (ReplicationException e) {
            this.logger.error("Error while getting replication instances for changerequest [{}]", changeRequestId, e);
        }
        return result;
    }

    protected <T extends AbstractChangeRequestEventReplicationSenderMessage> void sendMessage(T message,
        String changeRequestId)
    {
        List<ReplicationInstance> instances = this.getInstances(changeRequestId);
        if (!instances.isEmpty()) {
            try {
                this.replicationSenderProvider.get().send(message, instances);
            } catch (ReplicationException e) {
                this.logger.error("Error while sending the replication message [{}] for change request [{}]", message,
                    changeRequestId, e);
            }
        }
    }

    protected ComponentManager getComponentManager()
    {
        return this.componentManager;
    }

    public abstract void processLocalEvent(Event event, Object source, Object data);
}
