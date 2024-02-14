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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestCreatedRecordableEvent;
import org.xwiki.eventstream.Event;
import org.xwiki.notifications.CompositeEvent;

/**
 * Grouping strategy to ensure that create notification are never grouped.
 *
 * @version $Id$
 * @since 1.14
 */
@Component(roles = ChangeRequestGroupingStrategy.class)
@Singleton
public class ChangeRequestGroupingStrategy
{
    /**
     * Perform a custom grouping of the composite events. In practice this method only ensures that created event
     * are never grouped.
     * @param compositeEvent the composite event for which to perform a different grouping
     * @return a list of composite events
     */
    public List<CompositeEvent> groupEvents(CompositeEvent compositeEvent)
    {
        List<CompositeEvent> result;
        // We never want the create event to be grouped together.
        if (compositeEvent.getType().equals(ChangeRequestCreatedRecordableEvent.EVENT_NAME)) {
            result = new ArrayList<>();
            for (Event event : compositeEvent.getEvents()) {
                result.add(new CompositeEvent(event));
            }
        } else {
            result = List.of(compositeEvent);
        }
        return result;
    }
}
