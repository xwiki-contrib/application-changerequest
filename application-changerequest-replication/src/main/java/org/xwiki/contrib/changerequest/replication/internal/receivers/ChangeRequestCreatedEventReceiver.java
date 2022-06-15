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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.internal.ChangeRequestAutoWatchHandler;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestCreatedRecordableEvent;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.model.reference.DocumentReference;

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
    private DocumentAccessBridge documentAccessBridge;

    @Override
    public void receiveWithUserSet(ReplicationReceiverMessage message) throws ReplicationException
    {
        String changeRequestId = getChangeRequestId(message);

        try {
            Optional<ChangeRequest> changeRequestOpt = this.changeRequestStorageManager.load(changeRequestId);
            if (changeRequestOpt.isPresent()) {
                ChangeRequest changeRequest = changeRequestOpt.get();
                if (this.autoWatchHandler.shouldCreateWatchedEntity(changeRequest)) {
                    this.autoWatchHandler.watchChangeRequest(changeRequest);
                }
            }
        } catch (ChangeRequestException e) {
            throw new ReplicationException(
                String.format("Error when looking for change request [%s] for autowatch.", changeRequestId), e);
        }

        DocumentReference documentReference = getModifiedDocumentReference(message);
        try {
            DocumentModelBridge documentInstance =
                this.documentAccessBridge.getTranslatedDocumentInstance(documentReference);
            ChangeRequestCreatedRecordableEvent event =
                new ChangeRequestCreatedRecordableEvent(changeRequestId);
            this.recordableEventNotifier.notifyChangeRequestRecordableEvent(event, documentInstance);
        } catch (Exception e) {
            throw new ReplicationException(String.format(
                "Error when getting document for change request [%s] for event replication",
                changeRequestId), e);
        }
    }
}
