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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.script.ScriptContext;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.internal.converters.AbstractChangeRequestRecordableEventConverter;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestCreatedRecordableEvent;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestDiscussionRecordableEvent;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestFileChangeAddedRecordableEvent;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestRebasedRecordableEvent;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestReviewAddedRecordableEvent;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestStatusChangedRecordableEvent;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestUpdatedRecordableEvent;
import org.xwiki.contrib.changerequest.notifications.events.DocumentModifiedInChangeRequestEvent;
import org.xwiki.contrib.changerequest.notifications.events.StaleChangeRequestRecordableEvent;
import org.xwiki.eventstream.Event;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.notifiers.NotificationDisplayer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateManager;

/**
 * Component in charge of displaying the notifications for change request.
 *
 * @version $Id$
 * @since 0.3
 */
@Component
@Named("changerequest")
@Singleton
public class ChangeRequestNotificationDisplayer implements NotificationDisplayer
{
    private static final String EVENT_BINDING_NAME = "compositeEvent";
    private static final String CHANGE_REQUEST_REFERENCES_BINDING_NAME = "changeRequestReferences";
    private static final String TEMPLATE_PATH = "changerequest/%s.vm";
    private static final String EVENT_TYPE_PREFIX = "changerequest.";

    @Inject
    private NotificationDisplayer defaultNotificationDisplayer;

    @Inject
    private DocumentReferenceResolver<ChangeRequest> changeRequestDocumentReferenceResolver;

    @Inject
    private TemplateManager templateManager;

    @Inject
    private ScriptContextManager scriptContextManager;

    @Inject
    private Logger logger;

    private Map<Event, DocumentReference> getChangeRequestReferences(CompositeEvent compositeEvent)
        throws NotificationException
    {
        Map<Event, DocumentReference> result = new HashMap<>();
        for (Event event : compositeEvent.getEvents()) {
            Map<String, String> parameters = event.getParameters();
            if (parameters.containsKey(AbstractChangeRequestRecordableEventConverter.CHANGE_REQUEST_ID_PARAMETER_KEY)) {
                String crId =
                    parameters.get(AbstractChangeRequestRecordableEventConverter.CHANGE_REQUEST_ID_PARAMETER_KEY);
                // We don't load the change request on purpose here:
                // we still want it to be resolved even if it has been deleted, so that the notifications are displayed.
                ChangeRequest changeRequest = new ChangeRequest();
                changeRequest.setId(crId);
                DocumentReference changeRequestDocReference =
                    this.changeRequestDocumentReferenceResolver.resolve(changeRequest);
                result.put(event, changeRequestDocReference);
            } else {
                throw new NotificationException(
                    String.format("The event [%s] did not have the appropriate parameter to retrieve "
                        + "the change request.", event.getId()));
            }
        }
        return result;
    }

    @Override
    public Block renderNotification(CompositeEvent compositeEvent) throws NotificationException
    {
        Block result = new GroupBlock();
        String eventType = compositeEvent.getType();
        boolean shouldFallback = !eventType.contains(EVENT_TYPE_PREFIX);
        Map<Event, DocumentReference> changeRequestReferences = null;
        try {
            changeRequestReferences = this.getChangeRequestReferences(compositeEvent);
        } catch (NotificationException e) {
            logger.warn("Error while trying to get change request references: [{}]",
                ExceptionUtils.getRootCauseMessage(e));
            shouldFallback = true;
        }
        if (shouldFallback) {
            result = this.defaultNotificationDisplayer.renderNotification(compositeEvent);
        } else {
            ScriptContext scriptContext = scriptContextManager.getScriptContext();

            if (eventType.contains(EVENT_TYPE_PREFIX)) {
                eventType = eventType.substring(EVENT_TYPE_PREFIX.length());
            }

            String templateName = String.format(TEMPLATE_PATH, eventType);
            try {
                scriptContext.setAttribute(EVENT_BINDING_NAME, compositeEvent, ScriptContext.ENGINE_SCOPE);
                scriptContext.setAttribute(CHANGE_REQUEST_REFERENCES_BINDING_NAME, changeRequestReferences,
                    ScriptContext.ENGINE_SCOPE);

                Template template = templateManager.getTemplate(templateName);
                if (template != null) {
                    result.addChildren(templateManager.execute(template).getChildren());
                } else {
                    logger.warn("Cannot find template [{}] the notification display will fallback on "
                            + "default displayer.", templateName);
                    result = this.defaultNotificationDisplayer.renderNotification(compositeEvent);
                }
            } catch (Exception e) {
                throw new NotificationException("Failed to render the notification.", e);
            } finally {
                scriptContext.removeAttribute(EVENT_BINDING_NAME, ScriptContext.ENGINE_SCOPE);
                scriptContext.removeAttribute(CHANGE_REQUEST_REFERENCES_BINDING_NAME, ScriptContext.ENGINE_SCOPE);
            }
        }
        return result;
    }

    @Override
    public List<String> getSupportedEvents()
    {
        return Arrays.asList(
            ChangeRequestCreatedRecordableEvent.EVENT_NAME,
            ChangeRequestFileChangeAddedRecordableEvent.EVENT_NAME,
            ChangeRequestReviewAddedRecordableEvent.EVENT_NAME,
            ChangeRequestStatusChangedRecordableEvent.EVENT_NAME,
            DocumentModifiedInChangeRequestEvent.EVENT_NAME,
            ChangeRequestDiscussionRecordableEvent.EVENT_NAME,
            ChangeRequestUpdatedRecordableEvent.EVENT_NAME,
            StaleChangeRequestRecordableEvent.EVENT_NAME,
            ChangeRequestRebasedRecordableEvent.EVENT_NAME
        );
    }
}
