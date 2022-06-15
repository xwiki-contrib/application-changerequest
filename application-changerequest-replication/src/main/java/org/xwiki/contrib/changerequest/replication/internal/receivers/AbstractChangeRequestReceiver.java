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

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.contrib.changerequest.internal.ChangeRequestRecordableEventNotifier;
import org.xwiki.contrib.changerequest.replication.internal.messages.AbstractChangeRequestEventReplicationSenderMessage;
import org.xwiki.contrib.changerequest.replication.internal.messages.AbstractRecordableChangeRequestEventReplicationSenderMessage;
import org.xwiki.contrib.replication.AbstractReplicationReceiver;
import org.xwiki.contrib.replication.InvalidReplicationMessageException;
import org.xwiki.contrib.replication.ReplicationException;
import org.xwiki.contrib.replication.ReplicationReceiverMessage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;

import com.xpn.xwiki.XWikiContext;

/**
 * Abstract implementation of a message receiver for change request events.
 * This class mainly provides utility methods to retrieve the document information from the message and the
 * change request identifier. It also automatically sets the context user that was stored in the message, so that
 * the event is triggered with the proper user.
 *
 * @version $Id$
 * @since 0.16
 */
public abstract class AbstractChangeRequestReceiver extends AbstractReplicationReceiver
{
    @Inject
    protected ChangeRequestRecordableEventNotifier recordableEventNotifier;

    @Inject
    protected DocumentAccessBridge documentAccessBridge;

    @Inject
    private DocumentReferenceResolver<String> stringDocumentReferenceResolver;

    @Inject
    private Provider<XWikiContext> contextProvider;

    /**
     * Retrieve the change request identifier that was stored in the message.
     *
     * @param message the message that was replicated.
     * @return a change request identifier.
     * @throws InvalidReplicationMessageException if the message does not hold the identifier.
     */
    protected String getChangeRequestId(ReplicationReceiverMessage message) throws InvalidReplicationMessageException
    {
        return this.messageReader.getMetadata(message,
            AbstractRecordableChangeRequestEventReplicationSenderMessage.CHANGE_REQUEST_ID_PARAMETER, true);
    }

    private DocumentReference getDataDocumentReference(ReplicationReceiverMessage message)
        throws InvalidReplicationMessageException
    {
        String serializedReference = this.messageReader.getMetadata(message,
            AbstractChangeRequestEventReplicationSenderMessage.DATA_DOCUMENT, true);
        return this.stringDocumentReferenceResolver.resolve(serializedReference);
    }

    /**
     * Retrieve and load the data document from the message: this document is the document that was used when the event
     * was triggered.
     *
     * @param message the message that was replicated.
     * @return the instance of the document that was triggered along with the event which created the message.
     * @throws ReplicationException if the document cannot be loaded or if the message does not hold its reference.
     */
    protected DocumentModelBridge getDataDocument(ReplicationReceiverMessage message)
        throws ReplicationException
    {
        DocumentReference documentReference = getDataDocumentReference(message);
        try {
            return this.documentAccessBridge.getTranslatedDocumentInstance(documentReference);
        } catch (Exception e) {
            throw new ReplicationException("Error while loading a document to trigger the notification", e);
        }
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

    /**
     * Create the right event from the message information and triggers it: the context user is already set when this
     * method is called.
     *
     * @param message the message from which to rebuild an event.
     * @throws ReplicationException in case of problem when getting information to rebuild the event.
     */
    protected abstract void receiveWithUserSet(ReplicationReceiverMessage message) throws ReplicationException;
}
