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
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.events.ApproversUpdatedEvent;
import org.xwiki.contrib.changerequest.notifications.events.ApproversUpdatedTargetableEvent;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.RemoteObservationManagerContext;

/**
 * Listener in charge of transforming a {@link ApproversUpdatedEvent} to a {@link ApproversUpdatedTargetableEvent}.
 *
 * @version $Id$
 * @since 0.10
 */
@Component
@Singleton
@Named(ApproversUpdatedEventListener.NAME)
public class ApproversUpdatedEventListener extends AbstractEventListener
{
    static final String NAME = "ApproversUpdatedEventListener";

    @Inject
    private ObservationManager observationManager;

    @Inject
    private RemoteObservationManagerContext remoteObservationManagerContext;

    /**
     * Default constructor.
     */
    public ApproversUpdatedEventListener()
    {
        super(NAME, Collections.singletonList(new ApproversUpdatedEvent()));
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (!this.remoteObservationManagerContext.isRemoteState()) {
            Set<String> target = (Set<String>) data;
            ApproversUpdatedTargetableEvent approversUpdatedTargetableEvent =
                new ApproversUpdatedTargetableEvent(target);
            this.observationManager.notify(approversUpdatedTargetableEvent,
                AbstractChangeRequestEventListener.EVENT_SOURCE, source);
        }
    }
}
