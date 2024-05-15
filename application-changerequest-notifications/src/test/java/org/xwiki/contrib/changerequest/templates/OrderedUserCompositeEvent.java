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
package org.xwiki.contrib.changerequest.templates;

import java.util.LinkedHashSet;
import java.util.Set;

import org.xwiki.eventstream.Event;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.CompositeEvent;

/**
 * A class to ensure that users are properly ordered in {@code #getUsers}.
 * This class should be removed when upgrading to a more recent version of XWiki as it's been fixed in
 * recent versions of CompositeEvent.
 *
 * @version $Id$
 * @since 1.14
 */
public class OrderedUserCompositeEvent extends CompositeEvent
{
    /**
     * Default constructor with an event.
     * @param event the event.
     */
    public OrderedUserCompositeEvent(Event event)
    {
        super(event);
    }

    /**
     * Default constructor with a composite event.
     * @param compositeEvent the composite event.
     */
    public OrderedUserCompositeEvent(CompositeEvent compositeEvent)
    {
        super(compositeEvent);
    }

    @Override
    public Set<DocumentReference> getUsers()
    {
        Set<DocumentReference> users = new LinkedHashSet<>();

        for (Event event : this.getEvents()) {
            users.add(event.getUser());
        }

        return users;
    }
}
