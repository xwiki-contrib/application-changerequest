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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestFileChangeAddedRecordableEvent;
import org.xwiki.contrib.changerequest.replication.internal.messages.FileChangeAddedReplicationSenderMessage;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.model.reference.DocumentReference;

@Component
@Singleton
@Named(ChangeRequestFileChangeAddedRecordableEvent.EVENT_NAME)
public class FileChangeAddedEventReceiver extends AbstractChangeRequestReceiver
{
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Override
    public void receiveWithUserSet(ReplicationReceiverMessage message) throws ReplicationException
    {
        String changeRequestId = getChangeRequestId(message);
        String fileChangeId =
            this.messageReader.getMetadata(message, FileChangeAddedReplicationSenderMessage.FILE_CHANGE_ID, true);
        DocumentReference modifiedDocumentReference = getModifiedDocumentReference(message);

        try {
            DocumentModelBridge modifiedDocument =
                this.documentAccessBridge.getTranslatedDocumentInstance(modifiedDocumentReference);
            ChangeRequestFileChangeAddedRecordableEvent event =
                new ChangeRequestFileChangeAddedRecordableEvent(changeRequestId, fileChangeId);
            this.recordableEventNotifier.notifyChangeRequestRecordableEvent(event, modifiedDocument);

            DocumentModelBridge changeRequestDocument =
                this.recordableEventNotifier.getChangeRequestDocument(changeRequestId);
            this.recordableEventNotifier.notifyChangeRequestRecordableEvent(event, changeRequestDocument);
        } catch (Exception e) {
            throw new ReplicationException("Error while loading a document to trigger the notification", e);
        }
    }
}
