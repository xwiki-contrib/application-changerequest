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

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestReviewAddedRecordableEvent;
import org.xwiki.contrib.changerequest.replication.internal.messages.ReviewAddedSenderMessage;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;

/**
 * Default implementation of a message receiver for {@link ChangeRequestReviewAddedRecordableEvent}.
 *
 * @version $Id$
 * @since 0.16
 */
@Component
@Singleton
@Named(ChangeRequestReviewAddedRecordableEvent.EVENT_NAME)
public class ReviewAddedEventReceiver extends AbstractChangeRequestReceiver
{
    @Override
    protected void receiveWithUserSet(ReplicationReceiverMessage message)
        throws ReplicationException
    {
        String changeRequestId = getChangeRequestId(message);
        String reviewId = this.messageReader.getMetadata(message, ReviewAddedSenderMessage.REVIEW_ID, true);
        String originalApprover =
            this.messageReader.getMetadata(message, ReviewAddedSenderMessage.ORIGINAL_APPROVER, false);

        ChangeRequestReviewAddedRecordableEvent event =
            new ChangeRequestReviewAddedRecordableEvent(changeRequestId, reviewId, originalApprover);
        DocumentModelBridge crDocument = this.getDataDocument(message);
        this.recordableEventNotifier.notifyChangeRequestRecordableEvent(event, crDocument);
    }
}
