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
import org.xwiki.stability.Unstable;
import org.xwiki.user.UserReference;

/**
 * Manager for the delegate approver mechanism: this mechanism allows to define some users who are not themselves
 * approvers, but have a delegation of role. Such users could perform review if the delegate approver mechanism is
 * enabled.
 *
 * @param <T> the type of entity for which to retrieve the approvers (see {@link ApproversManager}).
 *
 * @version $Id$
 * @since 0.13
 */
@Unstable
@Role
public interface DelegateApproverManager<T>
{
    /**
     * Compute, save and retrieve the set of delegate users of the given user, based on the XWikiUsers properties.
     * Note that this method automatically removes the delegate object and replaces it by the result of the computation
     * so that it contains exactly what is returned by the method.
     * Also if the delegate approval mechanism is disabled, or if the list of properties is empty in the configuration
     * (see {@link ChangeRequestConfiguration#getDelegateClassPropertyList()}) then this method returns an empty set.
     *
     * @param userReference the reference for which to compute and save the delegate users
     * @return a set of users who are entitled to perform review on behalf of the given user, or an empty set
     * @throws ChangeRequestException in case of problem to compute the delegate users
     */
    Set<UserReference> computeDelegates(UserReference userReference) throws ChangeRequestException;

    /**
     * Retrieve the delegate users of the given user reference. This method does not perform a computation, but directly
     * retrieve the value of delegate users stored in an object.
     * Note that this method returns an empty set if the delegate mechanism is disabled.
     *
     * @param userReference the user for which to retrieve the delegate users
     * @return a set of delegate user or an empty set
     * @throws ChangeRequestException in case of problem to read user document
     */
    Set<UserReference> getDelegates(UserReference userReference) throws ChangeRequestException;

    /**
     * Check if the given user is a delegate of one of the approvers of the given entity. This method algorithm consists
     * in:
     * <ol>
     *     <li>retrieve the list of approvers of the entity based on {@link ApproversManager}</li>
     *     <li>for each approver, check the delegate approvers based on {@link #getDelegates(UserReference)}</li>
     *     <li>check if the given user belongs to one of the set</li>
     * </ol>
     *
     * @param userReference the user who might be a delegate approver of one of the approver of the entity
     * @param entity the entity for which to retrieve the approvers
     * @return {@code true} if the user is a delegate approver of one of the approver of the entity
     * @throws ChangeRequestException in case of problem when retrieving the list of approvers, or of delegate
     */
    boolean isDelegateApproverOf(UserReference userReference, T entity) throws ChangeRequestException;

    /**
     * Check if the given user is a delegate of the specific original approver of the given entity.
     * This method algorithm consists in:
     * <ol>
     *     <li>check if the given approver belongs to the list of approvers of the entity</li>
     *     <li>get the list of delegate for this approver</li>
     *     <li>check if the given user belongs to that list</li>
     * </ol>
     *
     * @param userReference the user who might be a delegate approver of the approver of the entity
     * @param entity the entity for which to check if the other user is an approver
     * @param originalApprover the user who might be a real approver
     * @return {@code true} if the user is a delegate approver of the approver of the entity
     * @throws ChangeRequestException in case of problem when retrieving the list of approvers, or of delegate
     */
    boolean isDelegateApproverOf(UserReference userReference, T entity, UserReference originalApprover)
        throws ChangeRequestException;

    /**
     * Retrieve all original approvers that the given user reference might be a delegate of, for the given entity.
     * This method algorithm consists in:
     * <ol>
     *     <li>get the list of approvers of the given entity</li>
     *     <li>for each approver, check if the given user is a delegate</li>
     *     <li>if it is a delegate, add the approver to the result</li>
     * </ol>
     *
     * @param userReference the delegate user for which to retrieve all possible approvers
     * @param entity the entity for which to retrieve the approvers
     * @return a set of user for whom the given user is entitled to review
     * @throws ChangeRequestException in case of problem when retrieving the list of approvers, or of delegate
     */
    Set<UserReference> getOriginalApprovers(UserReference userReference, T entity) throws ChangeRequestException;
}
