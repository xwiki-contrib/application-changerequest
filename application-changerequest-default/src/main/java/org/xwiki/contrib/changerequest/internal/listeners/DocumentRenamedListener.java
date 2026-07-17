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

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestManager;
import org.xwiki.contrib.changerequest.ReviewInvalidationReason;
import org.xwiki.contrib.changerequest.discussions.ChangeRequestDiscussionService;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.event.AbstractLocalEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.refactoring.event.DocumentRenamedEvent;
import org.xwiki.refactoring.job.MoveRequest;

/**
 * Listener in charge of refactoring change requests whenever a document is renamed.
 *
 * @version $Id$
 * @since 1.23
 */
@Component
@Singleton
@Named(DocumentRenamedListener.NAME)
public class DocumentRenamedListener extends AbstractLocalEventListener
{
    static final String NAME = "org.xwiki.contrib.changerequest.internal.listeners.DocumentRenamedListener";

    @Inject
    private JobProgressManager progressManager;

    @Inject
    private ChangeRequestStorageManager storageManager;

    @Inject
    private ChangeRequestManager changeRequestManager;

    @Inject
    private ChangeRequestDiscussionService changeRequestDiscussionService;

    @Inject
    private Logger logger;

    /**
     * Default constructor.
     */
    public DocumentRenamedListener()
    {
        super(NAME, new DocumentRenamedEvent());
    }

    @Override
    public void processLocalEvent(Event event, Object source, Object data)
    {
        MoveRequest request = (MoveRequest) data;
        boolean updateLinks = request.isUpdateLinks();

        if (updateLinks) {
            DocumentRenamedEvent renameEvent = (DocumentRenamedEvent) event;
            updateChangeRequests(renameEvent.getSourceReference(), renameEvent.getTargetReference(), request.isDeep());
        }
    }

    private void updateChangeRequests(DocumentReference source, DocumentReference target, boolean isDeep)
    {
        this.logger.info("Updating the change requests to refactor document [{}] to [{}].", source, target);

        List<ChangeRequest> changeRequests = List.of();

        try {
            changeRequests = this.storageManager.findChangeRequestTargeting(source);
            changeRequests =
                changeRequests.stream()
                    .filter(changeRequest -> changeRequest.getStatus().isOpen())
                    .collect(Collectors.toList());
        } catch (ChangeRequestException e) {
            this.logger.error("Failed to find change requests using document [{}].", source, e);
        }
        this.progressManager.pushLevelProgress(changeRequests.size(), this);
        for (ChangeRequest changeRequest : changeRequests) {
            this.progressManager.startStep(this);
            this.logger.info("Updating change request [{}].", changeRequest.getId());
            try {
                this.storageManager.refactorTargetEntity(changeRequest, source, target, isDeep);
                this.changeRequestManager.invalidateReviews(changeRequest, ReviewInvalidationReason.REFACTORING);
                this.changeRequestDiscussionService.refactorDiscussionFileReference(changeRequest.getId(), source,
                    target, isDeep);
            } catch (ChangeRequestException crE) {
                this.logger.error("Error while refactoring change request [{}] to move document [{}].",
                    changeRequest.getId(), source, crE);
            } finally {
                this.progressManager.endStep(this);
            }
        }
        this.progressManager.popLevelProgress(this);
    }
}
