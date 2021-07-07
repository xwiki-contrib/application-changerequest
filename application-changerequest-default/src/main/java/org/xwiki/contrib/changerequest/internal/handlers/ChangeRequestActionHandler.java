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
package org.xwiki.contrib.changerequest.internal.handlers;

import java.io.IOException;

import org.xwiki.component.annotation.Role;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestReference;

/**
 * Default interface for handling the various change request action.
 *
 * @version $Id$
 * @since 0.3
 */
@Role
public interface ChangeRequestActionHandler
{
    /**
     * Handle the given reference.
     *
     * @param changeRequestReference the reference to handle.
     * @throws ChangeRequestException in case of problem when handling the request.
     * @throws IOException in case of problem to write the response.
     */
    void handle(ChangeRequestReference changeRequestReference) throws ChangeRequestException, IOException;
}
