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
package org.xwiki.contrib.changerequest.internal.id;

import org.xwiki.component.annotation.Role;
import org.xwiki.contrib.changerequest.ChangeRequest;

/**
 * Component role dedicated to compute unique identifiers for {@link ChangeRequest}.
 * The idea of this component is to allow to easily switch from an id implementation to another.
 * Note that once an implementation is chosen, it shouldn't be changed since it impacts all already stored change
 * requests. Also note that those generated IDs are used in the URLs.
 *
 * @version $Id$
 * @since 0.1
 */
@Role
public interface ChangeRequestIDGenerator
{
    /**
     * Generate an identifier for change requests.
     * The generated identifier is not necessarily unique: it's the client job to check if it's unique or not.
     *
     * @param changeRequest the change request for which to generate an id.
     * @return an identifier for the change request.
     */
    String generateId(ChangeRequest changeRequest);
}
