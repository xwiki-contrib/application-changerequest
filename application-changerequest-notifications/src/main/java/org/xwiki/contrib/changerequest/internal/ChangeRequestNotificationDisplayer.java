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
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.script.ScriptContext;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestCreatedRecordableEvent;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestFileChangeAddedRecordableEvent;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestRebasedRecordableEvent;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestReviewAddedRecordableEvent;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestStatusChangedRecordableEvent;
import org.xwiki.contrib.changerequest.notifications.events.StaleChangeRequestRecordableEvent;
import org.xwiki.eventstream.Event;
import org.xwiki.model.reference.DocumentReference;
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
public class ChangeRequestNotificationDisplayer extends AbstractChangeRequestNotificationRenderer
    implements NotificationDisplayer
{
    private static final String EVENT_BINDING_NAME = "compositeEvent";
    private static final String TEMPLATE_PATH = "changerequest/alert/%s.vm";
    private static final String EVENT_TYPE_PREFIX = "changerequest.";

    @Inject
    private TemplateManager templateManager;

    @Inject
    private ScriptContextManager scriptContextManager;

    @Inject
    private ChangeRequestGroupingStrategy groupingStrategy;

    @Override
    public Block renderNotification(CompositeEvent originalCompositeEvent) throws NotificationException
    {
        Block result = new GroupBlock();
        List<CompositeEvent> compositeEvents = this.groupingStrategy.groupEvents(originalCompositeEvent);
        ScriptContext scriptContext = scriptContextManager.getScriptContext();

        for (CompositeEvent compositeEvent : compositeEvents) {
            Map<Event, DocumentReference> changeRequestReferences = getChangeRequestReferences(compositeEvent);
            String templateName = getTemplateName(compositeEvent.getType());
            scriptContext.setAttribute(EVENT_BINDING_NAME, compositeEvent, ScriptContext.ENGINE_SCOPE);
            scriptContext.setAttribute(CHANGE_REQUEST_REFERENCES_BINDING_NAME, changeRequestReferences,
                ScriptContext.ENGINE_SCOPE);

            try {
                Template template = templateManager.getTemplate(templateName);
                if (template != null) {
                    result.addChildren(templateManager.execute(template).getChildren());
                } else {
                    throw new NotificationException(String.format("Cannot find template [%s]", templateName));
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

    private String getTemplateName(String eventType)
    {
        String eventName = eventType;
        if (eventName.contains(EVENT_TYPE_PREFIX)) {
            eventName = eventName.substring(EVENT_TYPE_PREFIX.length());
        }

        return String.format(TEMPLATE_PATH, eventName);
    }

    @Override
    public List<String> getSupportedEvents()
    {
        return Arrays.asList(
            ChangeRequestCreatedRecordableEvent.EVENT_NAME,
            ChangeRequestFileChangeAddedRecordableEvent.EVENT_NAME,
            ChangeRequestReviewAddedRecordableEvent.EVENT_NAME,
            ChangeRequestStatusChangedRecordableEvent.EVENT_NAME,
            StaleChangeRequestRecordableEvent.EVENT_NAME,
            ChangeRequestRebasedRecordableEvent.EVENT_NAME
        );
    }
}
