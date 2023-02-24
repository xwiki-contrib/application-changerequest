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

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequestStatus;
import org.xwiki.contrib.changerequest.events.ChangeRequestStatusChangedEvent;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestStatusChangedRecordableEvent;
import org.xwiki.observation.event.Event;

/**
 * Listener responsible to handle {@link ChangeRequestStatusChangedEvent}.
 *
 * @version $Id$
 * @since 0.6
 */
@Component
@Named(ChangeRequestStatusChangedEventListener.NAME)
@Singleton
public class ChangeRequestStatusChangedEventListener extends AbstractChangeRequestEventListener
{
    static final String NAME = "ChangeRequestStatusChangedEventListener";

    /**
     * Default constructor.
     */
    public ChangeRequestStatusChangedEventListener()
    {
        super(NAME, Collections.singletonList(new ChangeRequestStatusChangedEvent()));
    }

    @Override
    public void processLocalEvent(Event event, Object source, Object data)
    {
        String changeRequestId = (String) source;
        try {
            DocumentModelBridge documentInstance = this.getChangeRequestDocument(changeRequestId);
            ChangeRequestStatus[] statuses = (ChangeRequestStatus[]) data;
            ChangeRequestStatusChangedRecordableEvent recordableEvent =
                new ChangeRequestStatusChangedRecordableEvent(changeRequestId, statuses[0], statuses[1]);
            this.notifyChangeRequestRecordableEvent(recordableEvent, documentInstance);
        } catch (Exception e) {
            this.logger.error(
                "Error while getting the document instance from [{}] after a change request status changed event: [{}]",
                source, ExceptionUtils.getRootCauseMessage(e)
            );
        }
    }
}
