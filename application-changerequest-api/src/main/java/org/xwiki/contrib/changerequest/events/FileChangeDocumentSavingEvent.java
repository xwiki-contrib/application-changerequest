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

import org.xwiki.observation.event.BeginEvent;
import org.xwiki.stability.Unstable;

/**
 * This event is triggered when a {@link org.xwiki.contrib.changerequest.FileChange} is about to be saved.
 * The corresponding end event is {@link FileChangeDocumentSavedEvent}.
 * This event should be listened to if the document subject to changes should be modified before being stored in the
 * change request. It can also allow to perform modification in the filechange itself.
 *
 * The event also send the following parameters:
 * <ul>
 *     <li>source: the {@link org.xwiki.contrib.changerequest.FileChange} about to be saved</li>
 *     <li>data: the XWikiDocument where the filechange will be saved. Note that this document does not yet contain
 *         the data from the filechange when this event is triggered.</li>
 * </ul>
 *
 * @version $Id$
 * @since 1.2
 */
@Unstable
public class FileChangeDocumentSavingEvent implements BeginEvent
{
    @Override
    public boolean matches(Object otherEvent)
    {
        return otherEvent instanceof FileChangeDocumentSavingEvent;
    }
}
