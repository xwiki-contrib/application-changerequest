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
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
     * @deprecated Since 1.12 prefer using {@link #save(ChangeRequest, String)}.
     */
    @Deprecated(since = "1.12")
    default void save(ChangeRequest changeRequest) throws ChangeRequestException
    {
        save(changeRequest, "");
    }

    /**
     * Save the given change request and all the related {@link org.xwiki.contrib.changerequest.FileChange}.
     * @param changeRequest the change request to save.
     * @param saveComment the comment to use for saving the change request document.
     * @throws ChangeRequestException in case of problem during the save.
     * @since 1.12
     */
    void save(ChangeRequest changeRequest, String saveComment) throws ChangeRequestException;

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
     * Note that merging a change request will trigger
     * {@link org.xwiki.contrib.changerequest.events.ChangeRequestMergingEvent} before the merging start and
     * {@link org.xwiki.contrib.changerequest.events.ChangeRequestMergedEvent} when it's done.
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
     * Find the document reference of all change requests that contains a file change for the given reference.
     * This method should be used instead of {@link #findChangeRequestTargeting(DocumentReference)} whenever the change
     * request information won't be used directly: it avoids loading in memory lots of information.
     *
     * @param documentReference the reference of the document subject of a change.
     * @return a list of references of change requests.
     * @throws ChangeRequestException in case of problem to execute the query.
     * @since 0.14
     */
    default List<DocumentReference> findChangeRequestReferenceTargeting(DocumentReference documentReference)
        throws ChangeRequestException
    {
        return Collections.emptyList();
    }

    /**
     * Find all change requests that are opened (i.e. not merged, or closed) and that have been created or updated
     * before the given limit date. The goal of this method is mainly to retrieve the old change requests that might
     * be stale.
     *
     * @param limitDate the date to consider in the query for getting change requests.
     * @param considerCreationDate {@code true} to use the creation date in the query, {@code false} to use the update
     *                             date.
     * @return a list of change requests matching the criteria.
     * @throws ChangeRequestException in case of problem to find the change requests.
     * @since 0.10
     */
    default List<ChangeRequest> findOpenChangeRequestsByDate(Date limitDate, boolean considerCreationDate)
        throws ChangeRequestException
    {
        return Collections.emptyList();
    }

    /***
     * Count the total number of change requests: this method mainly aims at being used when getting the change requests
     * with {@link #getChangeRequests(boolean, int, int)}.
     *
     * @param onlyOpen {@code true} to only consider the open ones (not merged or closed), {@code false} to consider
     *                  all of them.
     * @return the number of existing change requests
     * @throws ChangeRequestException in case of problem to execute the query
     * @since 0.14
     */
    default long countChangeRequests(boolean onlyOpen) throws ChangeRequestException
    {
        return -1;
    }

    /**
     * Retrieve all the change requests.
     *
     * @param onlyOpen {@code true} to only retrieve the open change requests, {@code false} to get all of them
     *                 (even the merged and closed)
     * @param offset where to start getting them
     * @param limit the limit number of results to return
     * @return a list of change requests
     * @throws ChangeRequestException in case of problem to perform the query
     * @since 0.14
     */
    default List<ChangeRequest> getChangeRequests(boolean onlyOpen, int offset, int limit) throws ChangeRequestException
    {
        return Collections.emptyList();
    }

    /**
     * Retrieve all the change requests references.
     * Contrarily to {@link #getChangeRequests(boolean, int, int)} this method doesn't load the change request but only
     * returns their document references.
     *
     * @param onlyOpen {@code true} to only retrieve the open change requests, {@code false} to get all of them
     *                 (even the merged and closed)
     * @param offset where to start getting them
     * @param limit the limit number of results to return
     * @return a list of change requests references
     * @throws ChangeRequestException in case of problem to perform the query
     * @since 0.14
     */
    default List<DocumentReference> getChangeRequestsReferences(boolean onlyOpen, int offset, int limit)
        throws ChangeRequestException
    {
        return Collections.emptyList();
    }

    /**
     * Find all change requests that are opened and that have been marked as staled before the given date.
     * @param limitDate the date before which the change request should have been flagged as staled.
     * @return a list of change requests matching the criteria.
     * @throws ChangeRequestException in case of problem to find the change requests.
     * @since 0.10
     */
    default List<ChangeRequest> findChangeRequestsStaledBefore(Date limitDate)
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
    default List<DocumentReference> getOpenChangeRequestMatchingName(String title) throws ChangeRequestException
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

    /**
     * Allow to split a change request and to ignore in purpose some changes of that change request: this allows to
     * remove on purpose changes related to a document from a change request.
     *
     * @param changeRequest the change request to split
     * @param changesToIgnore the list of references of changes that should be ignored during the split: be aware
     *                        that it means those changes will be lost
     * @return a list of change requests created by the split
     * @throws ChangeRequestException in case of problem during the splitting.
     * @see #split(ChangeRequest)
     * @since 1.11
     */
    default List<ChangeRequest> split(ChangeRequest changeRequest, Set<DocumentReference> changesToIgnore)
        throws ChangeRequestException
    {
        return split(changeRequest);
    }

    /**
     * Save the stale date of the change request. This method does not save any other information of the change request.
     * @param changeRequest the change request for which to save the stale date.
     * @throws ChangeRequestException in case of problem during the save.
     * @since 0.10
     */
    default void saveStaleDate(ChangeRequest changeRequest) throws ChangeRequestException
    {
    }

    /**
     * Delete the given change request and all related information including filechanges and discussions.
     *
     * @param changeRequest the change request to be deleted
     * @throws ChangeRequestException in case of problem during the deletion
     * @since 1.11
     */
    default void delete(ChangeRequest changeRequest) throws ChangeRequestException
    {
    }
}
