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

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.events.ChangeRequestCreatedEvent;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestCreatedRecordableEvent;
import org.xwiki.model.reference.DocumentReference;
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
    private static final List<Event> EVENT_LIST = Collections.singletonList(new ChangeRequestCreatedEvent());

    @Inject
    private ObservationManager observationManager;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

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
        if (source instanceof DocumentReference) {
            try {
                DocumentModelBridge documentInstance =
                    this.documentAccessBridge.getTranslatedDocumentInstance((DocumentReference) source);
                this.observationManager
                    .notify(new ChangeRequestCreatedRecordableEvent(), EVENT_SOURCE, documentInstance);
            } catch (Exception e) {
                this.logger.error(
                    "Error while getting the document instance from [{}] after a created change request event: [{}]",
                    source, ExceptionUtils.getRootCauseMessage(e)
                );
            }
        }
    }
}
