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
import org.xwiki.contrib.changerequest.discussions.ChangeRequestDiscussionEvent;
import org.xwiki.contrib.changerequest.discussions.references.AbstractChangeRequestDiscussionContextReference;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestDiscussionRecordableEvent;
import org.xwiki.observation.event.Event;

/**
 * Listener responsible to handle {@link ChangeRequestDiscussionEvent}.
 *
 * @version $Id$
 * @since 0.6
 */
@Component
@Named(ChangeRequestDiscussionEventListener.NAME)
@Singleton
public class ChangeRequestDiscussionEventListener extends AbstractChangeRequestEventListener
{
    static final String NAME = "ChangeRequestDiscussionEventListener";

    /**
     * Default constructor.
     */
    public ChangeRequestDiscussionEventListener()
    {
        super(NAME, Collections.singletonList(new ChangeRequestDiscussionEvent()));
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        String changeRequestId = (String) source;
        try {
            DocumentModelBridge documentInstance = this.getChangeRequestDocument(changeRequestId);
            AbstractChangeRequestDiscussionContextReference discussionContextReference =
                (AbstractChangeRequestDiscussionContextReference) data;
            ChangeRequestDiscussionRecordableEvent recordableEvent =
                new ChangeRequestDiscussionRecordableEvent(changeRequestId,
                    discussionContextReference.getType().name(),
                    discussionContextReference.getReference());
            this.notifyChangeRequestRecordableEvent(recordableEvent, documentInstance);
        } catch (Exception e) {
            this.logger.error(
                "Error while getting the document instance from [{}] after a change request discussion event: [{}]",
                source, ExceptionUtils.getRootCauseMessage(e)
            );
        }
    }
}
