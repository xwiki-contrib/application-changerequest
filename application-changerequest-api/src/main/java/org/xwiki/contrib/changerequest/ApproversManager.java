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

import java.util.Set;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;
import org.xwiki.user.UserReference;

/**
 * Allow to handle the approvers of a document or a change request.
 * The approvers can be explicitely defined in an xobject or can be inferred from rights.
 *
 * @param <T> this should be either a {@link DocumentReference} or a {@link ChangeRequest}.
 *
 * @version $Id$
 * @since 0.5
 */
@Unstable
@Role
public interface ApproversManager<T>
{
    /**
     * Check if the given user is an approver of the given entity.
     * Note an user could be an approver for different reasons:
     * <ul>
     *     <li>the user is explicitely listed as an approver of the entity</li>
     *     <li>the user belongs to a group which is explicitely listed as an approver of the entity</li>
     *     <li>there's no explicit approvers (group or users) for the entity, but the user own the right to perform
     *     approvals on the entity (be it directly, or through a group: in such case we consider the user is an implicit
     *     approver</li>
     * </ul>
     *
     * @param user the reference of the user for whom to perform the check.
     * @param entity the entity to perform approval on.
     * @param explicitOnly {@code true} if the method only checks if the user is an explicit approver (note that we
     *                      consider that if a user belongs to a group listed as approver, it's still explicit),
     *                      {@code false} if it checks for implicit approvers if the list of explicit approvers is
     *                      empty.
     * @return {@code true} if the user is an approver of the given entity.
     * @throws ChangeRequestException in case of error when reading the values.
     */
    boolean isApprover(UserReference user, T entity, boolean explicitOnly) throws ChangeRequestException;

    /**
     * Define explicit users approvers for the given entity.
     *
     * @param users the users who should be explicit approvers.
     * @param entity the entity to perform approval on.
     * @throws ChangeRequestException in case of problem for writing the values.
     */
    void setUsersApprovers(Set<UserReference> users, T entity) throws ChangeRequestException;

    /**
     * Define explicit groups approvers for the given entity.
     *
     * @param groups the groups who should be explicit approvers.
     * @param entity the entity to perform approval on.
     * @throws ChangeRequestException in case of problem for writing the values.
     */
    void setGroupsApprovers(Set<DocumentReference> groups, T entity) throws ChangeRequestException;

    /**
     * Retrieve the list of all explicit approvers of an entity.
     * Note that this method can perform a recursive retrieval for approvers by going into groups.
     *
     * @param entity the entity for which to get approvers.
     * @param recursive if {@code false} only retrieve the explicitely listed users approvers, if {@code true} also
     *                  compute the list of users approvers from groups approvers.
     * @return a set of user reference who are explicit approvers of the given entity.
     * @throws ChangeRequestException in case of error when reading the values.
     */
    Set<UserReference> getAllApprovers(T entity, boolean recursive) throws ChangeRequestException;

    /**
     * Retrieve the list of groups that can approve the given entity.
     *
     * @param entity the entity for which to retrieve the approvers groups.
     * @return a set of document reference which represents the groups.
     * @throws ChangeRequestException in case of error when reading the values.
     */
    Set<DocumentReference> getGroupsApprovers(T entity) throws ChangeRequestException;
}
