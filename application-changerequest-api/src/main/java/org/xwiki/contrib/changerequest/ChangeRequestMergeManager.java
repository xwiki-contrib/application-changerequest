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

import java.util.List;
import java.util.Optional;

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Role;
import org.xwiki.diff.ConflictDecision;
import org.xwiki.stability.Unstable;
import org.xwiki.store.merge.MergeDocumentResult;

/**
 * Component dedicated for performing various merge operations in change request.
 *
 * @version $Id$
 * @since 0.11
 */
@Role
@Unstable
public interface ChangeRequestMergeManager
{
    /**
     * Check if the given file change expose conflicts with the current version of the documents.
     *
     * @param fileChange the change to be checked for conflicts.
     * @return {@code true} if it contains conflicts, {@code false} otherwise.
     * @throws ChangeRequestException in case of problem for detecting conflicts.
     */
    boolean hasConflicts(FileChange fileChange) throws ChangeRequestException;

    /**
     * Check if any of the latest filechange of the given change request expose conflicts with the current published
     * version of the document.
     * 
     * @param changeRequest the change request for which to check for conflicts.
     * @return {@code true} if any of the latest filechange contains a conflict, {@code false} if all latest filechanges
     *          have no conflicts.
     * @throws ChangeRequestException in case of problem for detecting conflicts.
     * @see #hasConflicts(FileChange)
     */
    boolean hasConflict(ChangeRequest changeRequest) throws ChangeRequestException;

    /**
     * Perform a merge without saving between the given filechange and latest published version of the document
     * and returns the {@link MergeDocumentResult} containing all needed information to see diff and/or handle
     * conflicts.
     *
     * @param fileChange the file change for which to perform a merge.
     * @return a {@link MergeDocumentResult} with information about the merge.
     * @throws ChangeRequestException in case of problem when retrieving the information.
     */
    ChangeRequestMergeDocumentResult getMergeDocumentResult(FileChange fileChange) throws ChangeRequestException;

    /**
     * Perform a merge and fix the conflicts with the provided decision. Note that this method lead to saving a new
     * file change with the conflict resolution.
     *
     * @param fileChange the file change for which to perform a merge.
     * @param resolutionChoice the global decision to make for fixing the conflicts.
     * @param conflictDecisionList the specific decisions to take for each conflict if
     *         {@link ConflictResolutionChoice#CUSTOM} was chosen.
     * @return {@code true} if the merge succeeded without creating any new conflicts, {@code false} if some conflicts
     *          remained.
     * @throws ChangeRequestException in case of error to perform the merge.
     */
    boolean mergeWithConflictDecision(FileChange fileChange, ConflictResolutionChoice resolutionChoice,
        List<ConflictDecision<?>> conflictDecisionList) throws ChangeRequestException;

    /**
     * Merge a given modified document in the given change request, without saving the result.
     *
     * @param modifiedDocument a document with changes not yet saved.
     * @param previousVersion the version of the document where the modifications have been started.
     * @param changeRequest an existing change request.
     * @return an empty optional if the change request did not contain any changes related to the given document, else
     *          returns an optional containing the result of the merge: this one can be checked for conflicts.
     * @throws ChangeRequestException in case of problem for detecting conflicts.
     */
    Optional<MergeDocumentResult> mergeDocumentChanges(DocumentModelBridge modifiedDocument, String previousVersion,
        ChangeRequest changeRequest) throws ChangeRequestException;


}
