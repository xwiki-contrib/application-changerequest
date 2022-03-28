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
package org.xwiki.contrib.changerequest.internal.listeners;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ApproversManager;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestStatus;
import org.xwiki.contrib.changerequest.events.ChangeRequestStatusChangedEvent;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestReadyForReviewTargetableEvent;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceSerializer;

/**
 * Listener dedicated to trigger notifications events for explicit approvers whenever a change request is marked as
 * ready for review.
 *
 * @version $Id$
 * @since 0.11
 */
@Component
@Singleton
@Named(ReadyForReviewChangeRequestNotifier.NAME)
public class ReadyForReviewChangeRequestNotifier extends AbstractEventListener
{
    static final String NAME = "org.xwiki.contrib.changerequest.internal.listeners.ReadyToReviewChangeRequestNotifier";

    @Inject
    private ChangeRequestStorageManager changeRequestStorageManager;

    @Inject
    private ApproversManager<ChangeRequest> changeRequestApproversManager;

    @Inject
    private UserReferenceSerializer<String> userReferenceSerializer;

    @Inject
    private ObservationManager observationManager;

    @Inject
    private DocumentReferenceResolver<ChangeRequest> changeRequestDocumentReferenceResolver;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private Logger logger;

    /**
     * Default constructor.
     */
    public ReadyForReviewChangeRequestNotifier()
    {
        super(NAME, Collections.singletonList(new ChangeRequestStatusChangedEvent()));
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        ChangeRequestStatus[] statuses = (ChangeRequestStatus[]) data;
        if (statuses != null && statuses.length == 2 && statuses[1] == ChangeRequestStatus.READY_FOR_REVIEW) {
            String changeRequestId = (String) source;
            try {
                Optional<ChangeRequest> optionalChangeRequest = this.changeRequestStorageManager.load(changeRequestId);
                optionalChangeRequest.ifPresent(this::notifyChangeRequestApprovers);
            } catch (ChangeRequestException e) {
                logger.error("Error while loading change request [{}] in order to notify explicit approvers",
                    changeRequestId, e);
            }
        }
    }

    private void notifyChangeRequestApprovers(ChangeRequest changeRequest)
    {
        try {
            Set<UserReference> allApprovers = this.changeRequestApproversManager.getAllApprovers(changeRequest, true);
            Set<String> serializedApprovers = allApprovers.stream()
                .map(this.userReferenceSerializer::serialize)
                .collect(Collectors.toSet());
            ChangeRequestReadyForReviewTargetableEvent event =
                new ChangeRequestReadyForReviewTargetableEvent(serializedApprovers);
            DocumentReference documentReference = this.changeRequestDocumentReferenceResolver.resolve(changeRequest);
            DocumentModelBridge document =
                this.documentAccessBridge.getTranslatedDocumentInstance(documentReference);
            this.observationManager.notify(event, AbstractChangeRequestEventListener.EVENT_SOURCE, document);
        } catch (ChangeRequestException e) {
            logger.error("Error while loading the list of explicit approvers.", e);
        } catch (Exception e) {
            logger.error("Error while loading change request document [{}]", changeRequest.getId(), e);
        }
    }
}
