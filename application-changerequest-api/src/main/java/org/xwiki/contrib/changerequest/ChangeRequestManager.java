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
package org.xwiki.contrib.changerequest;

import java.util.Optional;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Component responsible to perform business operations on change requests.
 *
 * @version $Id$
 * @since 0.1-SNAPSHOT
 */
@Role
@Unstable
public interface ChangeRequestManager
{
    /**
     * Retrieve the change request identified by the provided identifier.
     * @param id the identifier used to retrieve a change request.
     * @return {@link Optional#empty()} if the change request cannot be found, else an optional containing an instance
     *          of {@link ChangeRequest} for the given identifier.
     */
    Optional<ChangeRequest> getChangeRequest(String id);

    /**
     * Check if the given file change expose conflicts with the current version of the documents.
     *
     * @param fileChange the change to be checked for conflicts.
     * @return {@code true} if it contains conflicts, {@code false} otherwise.
     */
    boolean hasConflicts(FileChange fileChange) throws ChangeRequestException;
}
