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
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ApproversManager;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.events.ChangeRequestCreatedEvent;
import org.xwiki.contrib.changerequest.events.SplitBeginChangeRequestEvent;
import org.xwiki.contrib.changerequest.internal.ChangeRequestAutoWatchHandler;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestCreatedRecordableEvent;
import org.xwiki.observation.ObservationContext;
import org.xwiki.observation.event.Event;
import org.xwiki.user.UserReference;

/**
 * Component responsible to handle {@link ChangeRequestCreatedEvent}.
 *
 * This listener is responsible of 2 things: creating the dedicated {@link ChangeRequestCreatedRecordableEvent}, and
 * handling the autowatch mechanism for the created change request document.
 *
 * @version $Id$
 * @since 0.6
 */
@Component
@Named(ChangeRequestCreatedEventListener.NAME)
@Singleton
public class ChangeRequestCreatedEventListener extends AbstractChangeRequestEventListener
{
    static final String NAME = "ChangeRequestCreatedEventListener";

    @Inject
    private Provider<ChangeRequestAutoWatchHandler> autoWatchHandlerProvider;

    @Inject
    private Provider<ApproversManager<ChangeRequest>> approversManagerProvider;

    @Inject
    private ObservationContext observationContext;

    /**
     * Default constructor.
     */
    public ChangeRequestCreatedEventListener()
    {
        super(NAME, Collections.singletonList(new ChangeRequestCreatedEvent()));
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        String changeRequestId = (String) source;
        try {
            ChangeRequest changeRequest = (ChangeRequest) data;
            UserReference creator = changeRequest.getCreator();
            ChangeRequestAutoWatchHandler autoWatchHandler = this.autoWatchHandlerProvider.get();
            if (autoWatchHandler.shouldCreateWatchedEntity(changeRequest, creator)) {
                autoWatchHandler.watchChangeRequest(changeRequest, creator);
            }
            Set<UserReference> allApprovers = this.approversManagerProvider.get().getAllApprovers(changeRequest, false);
            allApprovers.forEach( userReference -> {
                if (autoWatchHandler.shouldCreateWatchedEntity(changeRequest, userReference)) {
                    try {
                        autoWatchHandler.watchChangeRequest(changeRequest, userReference);
                    } catch (ChangeRequestException e) {
                        this.logger.error(
                            "Error while handling autowatch for changerequest [{}] and approver [{}]: [{}]",
                            changeRequest, userReference,
                            ExceptionUtils.getRootCauseMessage(e)
                        );
                        this.logger.debug("Full stack trace: ", e);
                    }
                }
            });
            DocumentModelBridge documentInstance = this.documentAccessBridge.getTranslatedDocumentInstance(
                changeRequest.getModifiedDocuments().iterator().next());
            ChangeRequestCreatedRecordableEvent recordableEvent =
                new ChangeRequestCreatedRecordableEvent(changeRequestId);
            if (observationContext.isIn(new SplitBeginChangeRequestEvent())) {
                recordableEvent.setFromSplit(true);
            }
            this.notifyChangeRequestRecordableEvent(recordableEvent, documentInstance);
        } catch (Exception e) {
            this.logger.error(
                "Error while getting the document instance from [{}] after a created change request event: [{}]",
                source, ExceptionUtils.getRootCauseMessage(e)
            );
        }
    }
}
