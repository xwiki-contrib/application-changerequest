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
package org.xwiki.contrib.changerequest.replication.internal.receivers;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ApproversManager;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.internal.ChangeRequestAutoWatchHandler;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestCreatedRecordableEvent;
import org.xwiki.contrib.changerequest.replication.internal.messages.ChangeRequestCreatedReplicationSenderMessage;
import org.xwiki.contrib.changerequest.replication.internal.messages.FileChangeAddedReplicationSenderMessage;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.user.UserReference;

/**
 * Default implementation of a message receiver for {@link ChangeRequestCreatedRecordableEvent}.
 *
 * @version $Id$
 * @since 0.16
 */
@Component
@Named(ChangeRequestCreatedRecordableEvent.EVENT_NAME)
@Singleton
public class ChangeRequestCreatedEventReceiver extends AbstractChangeRequestReceiver
{
    @Inject
    private ChangeRequestStorageManager changeRequestStorageManager;

    @Inject
    private ChangeRequestAutoWatchHandler autoWatchHandler;

    @Inject
    private Provider<ApproversManager<ChangeRequest>> approversManagerProvider;

    @Inject
    private Logger logger;

    @Override
    public void receiveWithUserSet(ReplicationReceiverMessage message) throws ReplicationException
    {
        String changeRequestId = getChangeRequestId(message);
        String fileChangeId =
            this.messageReader.getMetadata(message, FileChangeAddedReplicationSenderMessage.FILE_CHANGE_ID, true);
        try {
            Optional<ChangeRequest> changeRequestOpt = this.changeRequestStorageManager.load(changeRequestId);
            if (changeRequestOpt.isPresent()) {
                ChangeRequest changeRequest = changeRequestOpt.get();
                if (this.autoWatchHandler.shouldCreateWatchedEntity(changeRequest, changeRequest.getCreator())) {
                    this.autoWatchHandler.watchChangeRequest(changeRequest, changeRequest.getCreator());
                }
                Set<UserReference> allApprovers =
                    this.approversManagerProvider.get().getAllApprovers(changeRequest, false);
                allApprovers.forEach(userReference -> {
                    if (autoWatchHandler.shouldCreateWatchedEntity(changeRequest, userReference)) {
                        try {
                            autoWatchHandler.watchChangeRequest(changeRequest, userReference);
                        } catch (ChangeRequestException e) {
                            this.logger.error(
                                "Error while handling autowatch for changerequest [{}] and approver [{}]: [{}]",
                                changeRequest, userReference,
                                ExceptionUtils.getRootCauseMessage(e)
                            );
                            this.logger.debug("Full stack trace: ", e);
                        }
                    }
                });
            }
        } catch (ChangeRequestException e) {
            throw new ReplicationException(
                String.format("Error when looking for change request [%s] for autowatch.", changeRequestId), e);
        }

        boolean isFromSplit = Boolean.parseBoolean(this.messageReader.getMetadata(message,
            ChangeRequestCreatedReplicationSenderMessage.FROM_SPLIT, true));
        ChangeRequestCreatedRecordableEvent event =
            new ChangeRequestCreatedRecordableEvent(changeRequestId, fileChangeId);
        event.setFromSplit(isFromSplit);

        DocumentModelBridge dataDocument = this.getDataDocument(message);
        this.recordableEventNotifier.notifyChangeRequestRecordableEvent(event, dataDocument);
    }
}
