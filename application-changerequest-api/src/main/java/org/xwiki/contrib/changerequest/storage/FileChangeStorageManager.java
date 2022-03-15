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

import java.util.List;
import java.util.Optional;

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Role;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.model.reference.DocumentReference;
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
    void save(FileChange fileChange) throws ChangeRequestException;

    /**
     * Load the given file change related to the given change request.
     *
     * @param changeRequest the change request that owns the given file change.
     * @param changedDocument the reference of a changed document for which to retrieve the file changes.
     * @return a file change instance or an empty optional if the file change cannot be found.
     * @throws ChangeRequestException in case of errors while loading the file change.
     * @since 0.3
     */
    List<FileChange> load(ChangeRequest changeRequest, DocumentReference changedDocument)
        throws ChangeRequestException;

    /**
     * Merge the given file change.
     *
     * @param fileChange the file change to merge.
     * @throws ChangeRequestException in case of errors while merging the file change.
     */
    void merge(FileChange fileChange) throws ChangeRequestException;

    /**
     * Rebase the given file change so the diff is made against current version of the document.
     *
     * @param fileChange the file change to rebase.
     * @throws ChangeRequestException in case of problem when saving the new file change.
     */
    void rebase(FileChange fileChange) throws ChangeRequestException;

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
     * Note that event if the document has been deleted, XWiki is always returning an instance of the document, so this
     * method should never return null.
     *
     * @param fileChange an instance of a file change.
     * @return a {@link DocumentModelBridge} corresponding to the latest published version of the document modified
     *          in the file change.
     * @throws ChangeRequestException in case of problem to retrieve the document.
     */
    DocumentModelBridge getCurrentDocumentFromFileChange(FileChange fileChange) throws ChangeRequestException;

    /**
     * Retrieve the document corresponding to the file change document before it has been modified. This method returns
     * an optional, because we cannot guarantee that the exact same version is retrieved: it might have been deleted
     * in the meantime. So it returns the version only if it's an exact match.
     *
     * @param fileChange an instance of a file change.
     * @return a {@link DocumentModelBridge} corresponding to document changed in the file change, but with the version
     *         used to perform the changes, or an {@link Optional#empty()} if the version has been lost.
     * @throws ChangeRequestException in case of problem to retrieve the document.
     */
    Optional<DocumentModelBridge> getPreviousDocumentFromFileChange(FileChange fileChange)
        throws ChangeRequestException;

    /**
     * Retrieve the document corresponding to the file change at a specific version.
     *
     * @param fileChange an instance of a file change.
     * @param version the version of the file to retrieve.
     * @return a {@link DocumentModelBridge} corresponding to document changed in the file change, but on given version.
     * @throws ChangeRequestException in case of problem to retrieve the document.
     * @since 0.3
     */
    default DocumentModelBridge getDocumentFromFileChange(FileChange fileChange, String version)
        throws ChangeRequestException
    {
        return null;
    }
}
