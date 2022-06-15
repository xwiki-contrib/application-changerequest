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

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestReadyForReviewTargetableEvent;
import org.xwiki.contrib.changerequest.replication.internal.messages.ReadyForReviewSenderMessage;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;

/**
 * Default implementation of a message receiver for {@link ChangeRequestReadyForReviewTargetableEvent}.
 *
 * @version $Id$
 * @since 0.16
 */
@Component
@Singleton
@Named(ChangeRequestReadyForReviewTargetableEvent.EVENT_NAME)
public class ReadyForReviewEventReceiver extends AbstractChangeRequestReceiver
{
    @Override
    protected void receiveWithUserSet(ReplicationReceiverMessage message) throws ReplicationException
    {
        DocumentModelBridge dataDocument = this.getDataDocument(message);
        String serializedTargets = this.messageReader.getMetadata(message, ReadyForReviewSenderMessage.TARGETS, true);
        Set<String> targets = Arrays.stream(StringUtils.split(serializedTargets, ",")).collect(Collectors.toSet());

        ChangeRequestReadyForReviewTargetableEvent event = new ChangeRequestReadyForReviewTargetableEvent(targets);
        this.recordableEventNotifier.notifyChangeRequestRecordableEvent(event, dataDocument);
    }
}
