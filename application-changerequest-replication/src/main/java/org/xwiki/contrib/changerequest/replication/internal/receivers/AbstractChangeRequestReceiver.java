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
import javax.inject.Provider;

import org.slf4j.Logger;
import org.xwiki.contrib.changerequest.internal.ChangeRequestRecordableEventNotifier;
import org.xwiki.contrib.changerequest.replication.internal.messages.AbstractChangeRequestEventReplicationSenderMessage;
import org.xwiki.contrib.changerequest.replication.internal.messages.ChangeRequestCreatedReplicationSenderMessage;
import org.xwiki.contrib.replication.AbstractReplicationReceiver;
import org.xwiki.contrib.replication.InvalidReplicationMessageException;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;

import com.xpn.xwiki.XWikiContext;

public abstract class AbstractChangeRequestReceiver extends AbstractReplicationReceiver
{
    @Inject
    protected ChangeRequestRecordableEventNotifier recordableEventNotifier;

    @Inject
    private DocumentReferenceResolver<String> stringDocumentReferenceResolver;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    protected Logger logger;

    protected String getChangeRequestId(ReplicationReceiverMessage message) throws InvalidReplicationMessageException
    {
        return this.messageReader.getMetadata(message,
            AbstractChangeRequestEventReplicationSenderMessage.CHANGE_REQUEST_ID_PARAMETER, true);
    }

    protected DocumentReference getModifiedDocumentReference(ReplicationReceiverMessage message)
        throws InvalidReplicationMessageException
    {
        String serializedReference = this.messageReader.getMetadata(message,
            ChangeRequestCreatedReplicationSenderMessage.MODIFIED_DOCUMENT, true);
        return this.stringDocumentReferenceResolver.resolve(serializedReference);
    }

    private DocumentReference getContextUser(ReplicationReceiverMessage message)
        throws InvalidReplicationMessageException
    {
        String serializedUserReference = this.messageReader.getMetadata(message,
            AbstractChangeRequestEventReplicationSenderMessage.CONTEXT_USER, true);
        return this.stringDocumentReferenceResolver.resolve(serializedUserReference);
    }

    @Override
    public void receive(ReplicationReceiverMessage message) throws ReplicationException
    {
        XWikiContext context = this.contextProvider.get();
        DocumentReference userReference = context.getUserReference();
        context.setUserReference(this.getContextUser(message));
        try {
            this.receiveWithUserSet(message);
        } finally {
            context.setUserReference(userReference);
        }
    }

    protected abstract void receiveWithUserSet(ReplicationReceiverMessage replicationReceiverMessage)
        throws ReplicationException;
}
