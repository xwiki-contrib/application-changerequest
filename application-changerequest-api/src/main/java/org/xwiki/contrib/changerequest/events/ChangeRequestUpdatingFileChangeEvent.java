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
package org.xwiki.contrib.changerequest.events;

import java.io.Serializable;

import org.xwiki.observation.event.BeginEvent;
import org.xwiki.stability.Unstable;

/**
 * This event is triggered whenever an operation will save a change request with some changes on its filechange: be it
 * the creation of a new change request, the add of a new filechange, the rebase of a filechange, the fix of a conflict
 * etc.
 * Basically any operation involving a save of a filechange and of the change request should trigger this event.
 * Note that this event is triggered before the actual operation starts.
 * The associated end event is {@link ChangeRequestUpdatedFileChangeEvent}.
 *
 * The event also send the following parameters:
 * <ul>
 *      <li>source: the change request identifier (empty string in case of the creation of a change request)</li>
 *      <li>data: the change request instance that will be saved ({@code null} in case of the creation of a change
 *      request or in case of a remote event)
 *      </li>
 * </ul>
 *
 * @version $Id$
 * @since 0.10
 */
@Unstable
public class ChangeRequestUpdatingFileChangeEvent implements BeginEvent, Serializable
{
    @Override
    public boolean matches(Object otherEvent)
    {
        return otherEvent instanceof ChangeRequestUpdatingFileChangeEvent;
    }
}
