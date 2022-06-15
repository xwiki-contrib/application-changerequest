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
package org.xwiki.contrib.changerequest.internal;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.eventstream.RecordableEvent;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.observation.ObservationManager;

/**
 * Utility component for sending recordable events with the appropriate source.
 *
 * @version $Id$
 * @since 0.16
 */
@Component(roles = ChangeRequestRecordableEventNotifier.class)
@Singleton
public class ChangeRequestRecordableEventNotifier
{
    /**
     * Default event source.
     */
    static final String EVENT_SOURCE = "org.xwiki.contrib.changerequest:application-changerequest-notifications";

    @Inject
    private ObservationManager observationManager;

    @Inject
    private Provider<ChangeRequestStorageManager> changeRequestStorageManager;

    @Inject
    private DocumentReferenceResolver<ChangeRequest> changeRequestDocumentReferenceResolver;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private Logger logger;

    /**
     * Notify an event using the change request source.
     *
     * @param event the event to notify
     * @param document the document to send as data of the event
     */
    public void notifyChangeRequestRecordableEvent(RecordableEvent event, DocumentModelBridge document)
    {
        this.observationManager.notify(event, EVENT_SOURCE, document);
    }

    /**
     * Retrieve the document instance holding the given change request identifier.
     *
     * @param changeRequestId the identifier for which to retrieve the change request.
     * @return the instance of the document holding the change request or {@code null} if no change request can be found
     *         matching the given identifier.
     * @throws Exception in case of problem to load the change request or to load the document.
     */
    public DocumentModelBridge getChangeRequestDocument(String changeRequestId) throws Exception
    {
        Optional<ChangeRequest> optionalChangeRequest =
            this.changeRequestStorageManager.get().load(changeRequestId);
        if (optionalChangeRequest.isPresent()) {
            ChangeRequest changeRequest = optionalChangeRequest.get();
            return this.documentAccessBridge.getTranslatedDocumentInstance(
                this.changeRequestDocumentReferenceResolver.resolve(changeRequest));
        } else {
            logger.error("Cannot find change request with identifier [{}]", changeRequestId);
            return null;
        }
    }
}
