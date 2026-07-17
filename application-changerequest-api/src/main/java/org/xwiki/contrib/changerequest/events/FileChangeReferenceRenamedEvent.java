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

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.event.Event;

/**
 * Event triggered whenever a refactoring operation impacts a filechange reference.
 * This should be used for any operation that needs to be done to compensate side effects of changing the target
 * reference of a filechange, e.g. refactoring of discussions. Note that there might be more filechange modified than
 * the original refactoring operation, since refactoring of a space might impact request of creation.
 * The source sent along with this event is the ID of a change request.
 *
 * @version $Id$
 * @since 1.23
 */
public class FileChangeReferenceRenamedEvent implements Event
{
    private final DocumentReference source;
    private final DocumentReference target;
    private final boolean isDeep;

    /**
     * Default constructor.
     */
    public FileChangeReferenceRenamedEvent()
    {
        this(null, null, false);
    }

    /**
     * Default constructor.
     *
     * @param source the source reference of the rename
     * @param target the target reference of the rename
     * @param isDeep {@code true} if it concerns a full space
     */
    public FileChangeReferenceRenamedEvent(DocumentReference source, DocumentReference target, boolean isDeep)
    {
        this.source = source;
        this.target = target;
        this.isDeep = isDeep;
    }

    /**
     * @return {@code true} if the original refactoring was about a full space
     */
    public boolean isDeep()
    {
        return isDeep;
    }

    /**
     * @return the source reference of the rename
     */
    public DocumentReference getSource()
    {
        return source;
    }

    /**
     * @return the target reference of the rename
     */
    public DocumentReference getTarget()
    {
        return target;
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        return otherEvent instanceof FileChangeReferenceRenamedEvent;
    }
}
