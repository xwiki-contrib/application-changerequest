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

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Role;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.stability.Unstable;

/**
 * The role for the components in charge of storing the file changes.
 *
 * @version $Id$
 * @since 0.1
 */
@Role
@Unstable
public interface FileChangeStorageManager
{
    /**
     * Save the given file change associated to the given change request.
     * @param fileChange the file change to save.
     * @throws ChangeRequestException in case of problem to convert the file change.
     */
    void saveFileChange(FileChange fileChange) throws ChangeRequestException;

    /**
     * Retrieve the modified file change in a {@link DocumentModelBridge} in order to be used in the diff APIs.
     *
     * @param fileChange an instance of a file change.
     * @return a {@link DocumentModelBridge} whose content matches the changes performed for the file change.
     * @throws ChangeRequestException in case of problem to retrieve the document.
     */
    DocumentModelBridge getModifiedDocumentFromFileChange(FileChange fileChange) throws ChangeRequestException;

    /**
     * Retrieve the document corresponding to the file change in its current latest version.
     *
     * @param fileChange an instance of a file change.
     * @return a {@link DocumentModelBridge} corresponding to the latest published version of the document modified
     *          in the file change.
     * @throws ChangeRequestException in case of problem to retrieve the document.
     */
    DocumentModelBridge getCurrentDocumentFromFileChange(FileChange fileChange) throws ChangeRequestException;

    /**
     * Retrieve the document corresponding to the file change document before it has been modified.
     *
     * @param fileChange an instance of a file change.
     * @return a {@link DocumentModelBridge} corresponding to document changed in the file change, but on latest version
     *          before it was modified in the file change.
     * @throws ChangeRequestException in case of problem to retrieve the document.
     */
    DocumentModelBridge getPreviousDocumentFromFileChange(FileChange fileChange) throws ChangeRequestException;
}
