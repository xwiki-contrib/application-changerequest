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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.events.ChangeRequestFileChangeAddedEvent;
import org.xwiki.contrib.changerequest.events.ChangeRequestRebasedEvent;
import org.xwiki.contrib.changerequest.events.FileChangeRebasedEvent;
import org.xwiki.contrib.changerequest.internal.cache.ChangeRequestTitleCacheManager;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

/**
 * Listener whose role is to properly invalidate entries of {@link ChangeRequestTitleCacheManager}.
 *
 * @version $Id$
 * @since 1.5
 */
@Component
@Named(TitleCacheInvalidationListener.NAME)
@Singleton
public class TitleCacheInvalidationListener extends AbstractEventListener
{
    static final String NAME = "org.xwiki.contrib.changerequest.internal.listeners.TitleCacheInvalidationListener";

    private static final List<Event> EVENT_LIST = List.of(
        new ChangeRequestFileChangeAddedEvent(),
        new FileChangeRebasedEvent(),
        new ChangeRequestRebasedEvent()
    );

    @Inject
    private Provider<ChangeRequestTitleCacheManager> titleCacheManagerProvider;

    /**
     * Default constructor.
     */
    public TitleCacheInvalidationListener()
    {
        super(NAME, EVENT_LIST);
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        String changeRequestId = (String) source;
        if (event instanceof ChangeRequestRebasedEvent) {
            this.titleCacheManagerProvider.get().invalidate(changeRequestId);
        } else {
            FileChange fileChange = (FileChange) data;
            this.titleCacheManagerProvider.get().invalidate(changeRequestId, fileChange);
        }
    }
}
