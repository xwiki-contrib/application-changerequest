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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.xwiki.component.annotation.Role;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
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

    /**
     * Find all change requests that contains a file change for the given reference.
     *
     * @param documentReference the file targeted by a change request.
     * @return a list of change request.
     * @throws ChangeRequestException in case of problem to find the change requests.
     * @since 0.3
     */
    default List<ChangeRequest> findChangeRequestTargeting(DocumentReference documentReference)
        throws ChangeRequestException
    {
        return Collections.emptyList();
    }

    /**
     * Find all change requests that contains a file change inside the given reference.
     *
     * @param spaceReference reference of a space that might be targeted by a change
     * @return a list of change request.
     * @throws ChangeRequestException in case of problem to find the change requests.
     * @since 0.7
     */
    default List<ChangeRequest> findChangeRequestTargeting(SpaceReference spaceReference)
        throws ChangeRequestException
    {
        return Collections.emptyList();
    }

    /**
     * Search for change requests document references that are matching the given title.
     *
     * @param title a partial title for finding change requests.
     * @return a list of document references that are corresponding to a change request.
     * @throws ChangeRequestException in case of problem to find the change requests.
     * @since 0.3
     */
    default List<DocumentReference> getChangeRequestMatchingName(String title) throws ChangeRequestException
    {
        return Collections.emptyList();
    }

    /**
     * Allow to split a change request in as many change requests as there is modified documents in it.
     * Each splitted change request will only contain the file changes corresponding to one document reference,
     * global comments and reviews are copied in all new change requests, specific comments regarding documents will be
     * placed in specific change requests. The original change request is deleted at the end of the operation.
     *
     * @param changeRequest the change request to split
     * @return a list of change requests created by the split
     * @throws ChangeRequestException in case of problem during the splitting.
     * @since 0.7
     */
    default List<ChangeRequest> split(ChangeRequest changeRequest) throws ChangeRequestException
    {
        return Collections.emptyList();
    }
}
