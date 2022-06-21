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
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.events.SplitChangeRequestEvent;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestCreatedRecordableEvent;
import org.xwiki.observation.event.Event;

/**
 * Listener of {@link SplitChangeRequestEvent} that triggers {@link ChangeRequestCreatedRecordableEvent} for all
 * new change request created by the split.
 *
 * @version $Id$
 * @since 0.14
 */
@Component
@Singleton
@Named(SplitChangeRequestEventListener.NAME)
public class SplitChangeRequestEventListener extends AbstractChangeRequestEventListener
{
    static final String NAME = "SplitChangeRequestEventListener";

    /**
     * Default constructor.
     */
    public SplitChangeRequestEventListener()
    {
        super(NAME, Collections.singletonList(new SplitChangeRequestEvent()));
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        List<ChangeRequest> newChangeRequests = (List<ChangeRequest>) data;

        for (ChangeRequest newChangeRequest : newChangeRequests) {
            String changeRequestId = newChangeRequest.getId();
            ChangeRequestCreatedRecordableEvent recordableEvent =
                new ChangeRequestCreatedRecordableEvent(changeRequestId);
            recordableEvent.setFromSplit(true);
            try {
                DocumentModelBridge changeRequestDocument = this.getChangeRequestDocument(changeRequestId);
                this.notifyChangeRequestRecordableEvent(recordableEvent, changeRequestDocument);
            } catch (Exception e) {
                this.logger.error("Error while loading document for change request [{}]", changeRequestId, e);
            }
        }
    }
}
