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
package org.xwiki.contrib.changerequest.discussions.internal;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.discussions.ChangeRequestDiscussionEvent;
import org.xwiki.contrib.changerequest.discussions.ChangeRequestDiscussionException;
import org.xwiki.contrib.changerequest.discussions.ChangeRequestDiscussionService;
import org.xwiki.contrib.changerequest.discussions.references.AbstractChangeRequestDiscussionContextReference;
import org.xwiki.contrib.discussions.domain.Message;
import org.xwiki.contrib.discussions.events.ActionType;
import org.xwiki.contrib.discussions.events.MessageEvent;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.RemoteObservationManagerContext;

/**
 * Listener whose purpose is to check for message creation and to trigger {@link ChangeRequestDiscussionEvent}.
 *
 * @version $Id$
 * @since 0.6
 */
@Component
@Named(MessageCreationListener.NAME)
@Singleton
public class MessageCreationListener extends AbstractEventListener
{
    static final String NAME = "org.xwiki.contrib.changerequest.discussions.internal.MessageCreationListener";

    private static final List<Event> EVENT_LIST = Collections.singletonList(new MessageEvent(ActionType.CREATE));

    @Inject
    private Provider<ChangeRequestDiscussionService> changeRequestDiscussionServiceProvider;

    @Inject
    private Logger logger;

    @Inject
    private ObservationManager observationManager;

    @Inject
    private RemoteObservationManagerContext remoteObservationManagerContext;

    /**
     * Default constructor.
     */
    public MessageCreationListener()
    {
        super(NAME, EVENT_LIST);
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (!this.remoteObservationManagerContext.isRemoteState()
            && ChangeRequestDiscussionService.APPLICATION_HINT.equals(source)) {
            Message message = (Message) data;
            try {
                AbstractChangeRequestDiscussionContextReference reference =
                    this.changeRequestDiscussionServiceProvider.get().getReferenceFrom(message.getDiscussion());
                this.observationManager
                    .notify(new ChangeRequestDiscussionEvent(), reference.getChangeRequestId(), message);
            } catch (ChangeRequestDiscussionException e) {
                logger.warn("Error while computing reference for message [{}]: [{}]", message,
                    ExceptionUtils.getRootCauseMessage(e));
            }
        }
    }
}
