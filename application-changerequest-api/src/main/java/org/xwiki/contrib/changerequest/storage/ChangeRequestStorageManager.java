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
package org.xwiki.contrib.changerequest.storage;

import java.util.Optional;

import org.xwiki.component.annotation.Role;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.stability.Unstable;
/**
 * Define the API for the storage manager of change request.
 *
 * @version $Id$
 * @since 0.1
 */
@Role
@Unstable
public interface ChangeRequestStorageManager
{
    /**
     * Save the given change request and all the related {@link org.xwiki.contrib.changerequest.FileChange}.
     * @param changeRequest the change request to save.
     * @throws ChangeRequestException in case of problem during the save.
     */
    void save(ChangeRequest changeRequest) throws ChangeRequestException;

    /**
     * Load a change request based on the given identifier.
     *
     * @param changeRequestId the id of a change request to find.
     * @return a change request instance or an empty optional if it cannot be found.
     * @throws ChangeRequestException in case of errors while loading.
     */
    Optional<ChangeRequest> load(String changeRequestId) throws ChangeRequestException;

    /**
     * Merge the given change request changes.
     *
     * @param changeRequest the change request to merge.
     * @throws ChangeRequestException in case of errors during the merge.
     */
    void merge(ChangeRequest changeRequest) throws ChangeRequestException;
}
