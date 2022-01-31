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

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;

/**
 * Perform checks on the new file change to add in a change request.
 *
 * @version $Id$
 * @since 0.9
 */
@Unstable
@Role
public interface FileChangeCompatibilityChecker
{
    /**
     * Check if the given document reference can be added to the given change request.
     *
     * @param changeRequest the change request in which to add new chnages.
     * @param documentReference the reference of the document with new changes.
     * @return {@code true} if the document can be added to the change request, {@code false} it there's an
     *          incompatibility.
     */
    boolean canChangeOnDocumentBeAdded(ChangeRequest changeRequest, DocumentReference documentReference);
}