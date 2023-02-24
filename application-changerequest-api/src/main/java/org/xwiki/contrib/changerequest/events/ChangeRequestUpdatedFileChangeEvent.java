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

import org.xwiki.observation.event.EndEvent;

/**
 * This event is triggered whenever an operation will save a change request with some changes on its filechange: be it
 * the creation of a new change request, the add of a new filechange, the rebase of a filechange, the fix of a conflict
 * etc.
 * Basically any operation involving a save of a filechange and of the change request should trigger this event.
 * Note that this event is triggered after the actual operation finished.
 * The associated begin event is {@link ChangeRequestUpdatingFileChangeEvent}.
 *
 * The event also send the following parameters:
 * <ul>
 *      <li>source: the change request identifier</li>
 *      <li>data: the file change instance that has been saved or the change request instance when we cannot identify a
 *                single filechange save (e.g. in case of a rebase of an entire change request)</li>
 * </ul>
 *
 * @version $Id$
 * @since 0.10
 */
public class ChangeRequestUpdatedFileChangeEvent implements EndEvent, Serializable
{
    @Override
    public boolean matches(Object otherEvent)
    {
        return otherEvent instanceof ChangeRequestUpdatedFileChangeEvent;
    }
}
