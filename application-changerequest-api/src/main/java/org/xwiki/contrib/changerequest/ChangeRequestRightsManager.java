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

/**
 * Component responsible to handle the rights synchronization between change requests and modified documents.
 * Note that this component is not related with {@link org.xwiki.contrib.changerequest.rights.ChangeRequestRight}
 * despite its name.
 *
 * @version $Id$
 * @since 0.7
 */
@Unstable
@Role
public interface ChangeRequestRightsManager
{
    /**
     * Copy all rights coming from the original change request to the target one, except view rights rules.
     *
     * @param originalChangeRequest the change request from which to get the rights to copy.
     * @param targetChangeRequest the change request where to copy the rights.
     * @throws ChangeRequestException in case of problems when reading or writing the rights.
     */
    void copyAllButViewRights(ChangeRequest originalChangeRequest, ChangeRequest targetChangeRequest)
        throws ChangeRequestException;

    /**
     * Determine if view access is still consistent if a new change related to the given document reference is added
     * to the given change request. The consistency here means that there's no rules that are contradictory between
     * the documents included in the change request, and the new document.
     *
     * @param changeRequest the change request where the new filechange should be added.
     * @param newChange the change to be added.
     * @return {@code true} if the given document reference can be added to the change request without creating right
     *          inconsistency.
     * @throws ChangeRequestException in case of problem to access the rights.
     */
    boolean isViewAccessConsistent(ChangeRequest changeRequest, DocumentReference newChange)
        throws ChangeRequestException;

    /**
     * Determine if the view right is still consistent in the change request for the given list of user references.
     * The consistency here consists only in defining if each user or group has independently same allow or deny access
     * of each document of the change request. This method should be called whenever a right is updated in a document.
     *
     * @param changeRequest the change request for which to check the consistency.
     * @param subjectReferences the users or groups for which to check the consistency of rights.
     * @return {@code true} if the view right is consistent across all document of the change request for each given
     *          user independently.
     * @throws ChangeRequestException in case of problem to access the rights.
     */
    boolean isViewAccessStillConsistent(ChangeRequest changeRequest, Set<DocumentReference> subjectReferences)
        throws ChangeRequestException;

    /**
     * Copy the view right rules coming from the given document reference to the change request.
     * Note that all inherited rules are copied too.
     *
     * @param changeRequest the change request in which to copy rights.
     * @param newChange the document reference from which to get rights to copy.
     * @throws ChangeRequestException in case of problem for accessing or copying rights.
     */
    void copyViewRights(ChangeRequest changeRequest, DocumentReference newChange) throws ChangeRequestException;
}
