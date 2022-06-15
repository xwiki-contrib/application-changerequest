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
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;

@Component(roles = ChangeRequestRecordableEventNotifier.class)
@Singleton
public class ChangeRequestRecordableEventNotifier
{
    /**
     * Default event source.
     */
    public static final String EVENT_SOURCE = "org.xwiki.contrib.changerequest:application-changerequest-notifications";

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

    public void notifyChangeRequestRecordableEvent(Event event,
        DocumentModelBridge document)
    {
        this.observationManager.notify(event, EVENT_SOURCE, document);
    }

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
