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
import javax.inject.Provider;

import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.contrib.changerequest.internal.ChangeRequestRecordableEventNotifier;
import org.xwiki.contrib.changerequest.internal.ChangeRequestTitleCacheManager;
import org.xwiki.contrib.changerequest.notifications.events.AbstractChangeRequestRecordableEvent;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.AbstractLocalEventListener;
import org.xwiki.observation.event.Event;

/**
 * Abstract implementation of the listeners we use to handle the various change request events.
 *
 * @version $Id$
 * @since 0.6
 */
public abstract class AbstractChangeRequestEventListener extends AbstractLocalEventListener
{
    @Inject
    protected Logger logger;

    @Inject
    protected DocumentAccessBridge documentAccessBridge;

    @Inject
    protected ChangeRequestRecordableEventNotifier changeRequestRecordableEventNotifier;

    @Inject
    protected Provider<ChangeRequestTitleCacheManager> titleCacheManagerProvider;

    /**
     * Default constructor.
     *
     * @param name the name of the listener.
     * @param events the events to listen to.
     */
    public AbstractChangeRequestEventListener(String name, List<? extends Event> events)
    {
        super(name, events);
    }

    protected void notifyChangeRequestRecordableEvent(AbstractChangeRequestRecordableEvent event,
        DocumentModelBridge document)
    {
        this.changeRequestRecordableEventNotifier.notifyChangeRequestRecordableEvent(event, document);
    }

    protected DocumentModelBridge getChangeRequestDocument(String changeRequestId) throws Exception
    {
        return this.changeRequestRecordableEventNotifier.getChangeRequestDocument(changeRequestId);
    }
}
