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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestReview;
import org.xwiki.contrib.changerequest.ChangeRequestStatus;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.discussions.ChangeRequestDiscussionEvent;
import org.xwiki.contrib.changerequest.discussions.references.AbstractChangeRequestDiscussionContextReference;
import org.xwiki.contrib.changerequest.events.ChangeRequestCreatedEvent;
import org.xwiki.contrib.changerequest.events.ChangeRequestFileChangeAddedEvent;
import org.xwiki.contrib.changerequest.events.ChangeRequestReviewAddedEvent;
import org.xwiki.contrib.changerequest.events.ChangeRequestStatusChangedEvent;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestCreatedRecordableEvent;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestDiscussionRecordableEvent;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestFileChangeAddedRecordableEvent;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestReviewAddedRecordableEvent;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestStatusChangedRecordableEvent;
import org.xwiki.contrib.changerequest.notifications.events.DocumentModifiedInChangeRequestEvent;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.eventstream.RecordableEvent;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;

/**
 * Listener that aims at transforming change request internal events to recordable events for notifications.
 *
 * @version $Id$
 * @since 0.1
 */
@Component
@Singleton
public class ChangeRequestEventsListener extends AbstractEventListener
{
    static final String NAME = "ChangeRequestEventsListener";

    /**
     * Default event source.
     */
    static final String EVENT_SOURCE = "org.xwiki.contrib.changerequest:application-changerequest-notifications";
    private static final List<Event> EVENT_LIST = Arrays.asList(
        new ChangeRequestCreatedEvent(),
        new ChangeRequestFileChangeAddedEvent(),
        new ChangeRequestStatusChangedEvent(),
        new ChangeRequestReviewAddedEvent(),
        new ChangeRequestDiscussionEvent()
    );

    @Inject
    private ObservationManager observationManager;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private ChangeRequestStorageManager changeRequestStorageManager;

    @Inject
    private DocumentReferenceResolver<ChangeRequest> changeRequestDocumentReferenceResolver;

    @Inject
    private Logger logger;

    /**
     * Default constructor.
     */
    public ChangeRequestEventsListener()
    {
        super(NAME, EVENT_LIST);
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        try {
            String changeRequestId = (String) source;
            DocumentModelBridge documentInstance = null;
            RecordableEvent recordableEvent = null;
            if (event instanceof ChangeRequestCreatedEvent) {
                ChangeRequest changeRequest = (ChangeRequest) data;
                documentInstance = this.documentAccessBridge.getTranslatedDocumentInstance(
                    changeRequest.getModifiedDocuments().iterator().next());
                recordableEvent = new ChangeRequestCreatedRecordableEvent(changeRequestId);
            } else if (event instanceof ChangeRequestFileChangeAddedEvent) {
                // We send two events in case of file change added:
                // one that targets the watchers of the document that has been modified
                // the other one that targets the watchers of the change request itself.
                FileChange fileChange = (FileChange) data;
                documentInstance = this.documentAccessBridge.getTranslatedDocumentInstance(
                    fileChange.getTargetEntity());
                this.observationManager.notify(new DocumentModifiedInChangeRequestEvent(changeRequestId), EVENT_SOURCE,
                    documentInstance);
                documentInstance = this.getChangeRequestDocument(changeRequestId);
                recordableEvent = new ChangeRequestFileChangeAddedRecordableEvent(changeRequestId, fileChange.getId());
            } else if (event instanceof ChangeRequestStatusChangedEvent) {
                documentInstance = this.getChangeRequestDocument(changeRequestId);
                ChangeRequestStatus[] statuses = (ChangeRequestStatus[]) data;
                recordableEvent = new ChangeRequestStatusChangedRecordableEvent(changeRequestId,
                    statuses[0], statuses[1]);
            } else if (event instanceof ChangeRequestReviewAddedEvent) {
                documentInstance = this.getChangeRequestDocument(changeRequestId);
                ChangeRequestReview review = (ChangeRequestReview) data;
                recordableEvent = new ChangeRequestReviewAddedRecordableEvent(changeRequestId, review.getId());
            } else if (event instanceof ChangeRequestDiscussionEvent) {
                documentInstance = this.getChangeRequestDocument(changeRequestId);
                AbstractChangeRequestDiscussionContextReference discussionContextReference =
                    (AbstractChangeRequestDiscussionContextReference) data;
                recordableEvent = new ChangeRequestDiscussionRecordableEvent(changeRequestId,
                    discussionContextReference.getType().name(),
                    discussionContextReference.getReference());
            }
            this.observationManager.notify(recordableEvent, EVENT_SOURCE, documentInstance);
        } catch (Exception e) {
            this.logger.error(
                "Error while getting the document instance from [{}] after a created change request event: [{}]",
                source, ExceptionUtils.getRootCauseMessage(e)
            );
        }
    }

    private DocumentModelBridge getChangeRequestDocument(String changeRequestId) throws Exception
    {
        Optional<ChangeRequest> optionalChangeRequest =
            this.changeRequestStorageManager.load(changeRequestId);
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
