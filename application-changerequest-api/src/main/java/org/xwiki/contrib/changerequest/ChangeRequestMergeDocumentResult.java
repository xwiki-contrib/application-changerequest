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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;
import org.xwiki.store.merge.MergeDocumentResult;

/**
 * Represents the result of a merge for change request.
 * In case of a {@link FileChange.FileChangeType#EDITION} this class contains the actual {@link MergeDocumentResult}
 * wrapped in it. In case of deletion, it contains only information if there's a conflict or not.
 * In all cases, it's possible to check if the current file change exposes a conflict, and the document title to use
 * for display purpose.
 *
 * @version $Id$
 * @since 0.5
 */
@Unstable
public class ChangeRequestMergeDocumentResult
{
    private final String fileChangeId;
    private final FileChange.FileChangeType type;
    private MergeDocumentResult wrappedResult;
    private final boolean isConflictingDeletion;
    private String documentTitle;
    private DocumentReference documentReference;

    /**
     * Constructor to use in case of {@link FileChange.FileChangeType#EDITION}.
     *
     * @param mergeDocumentResult the result of the merge that will be wrapped
     * @param fileChangeId the identifier of the file change used to compute this merge result
     */
    public ChangeRequestMergeDocumentResult(MergeDocumentResult mergeDocumentResult, String fileChangeId)
    {
        this.wrappedResult = mergeDocumentResult;
        this.type = FileChange.FileChangeType.EDITION;
        this.isConflictingDeletion = false;
        this.fileChangeId = fileChangeId;
        this.documentReference = mergeDocumentResult.getMergeResult().getDocumentReference();
    }

    /**
     * Constructor to use in case of {@link FileChange.FileChangeType#DELETION}.
     *
     * @param conflictingDeletion {@code true} if there's a conflict with this deletion
     * @param fileChangeId the identifier of the file change used to compute this merge result
     */
    public ChangeRequestMergeDocumentResult(boolean conflictingDeletion, String fileChangeId)
    {
        this.isConflictingDeletion = conflictingDeletion;
        this.type = FileChange.FileChangeType.DELETION;
        this.fileChangeId = fileChangeId;
    }

    /**
     * Check if this result exposes a conflict.
     * @return {@code true} if the wrapped result contains a conflict, or in case of deletion if a conflict
     *          was computed.
     */
    public boolean hasConflicts()
    {
        return (type == FileChange.FileChangeType.DELETION && this.isConflictingDeletion)
            || (type == FileChange.FileChangeType.EDITION && this.wrappedResult.hasConflicts());
    }

    /**
     * @return the document title to use for displaying the result.
     */
    public String getDocumentTitle()
    {
        return documentTitle;
    }

    /**
     * @param documentTitle the document title to use for displaying the result.
     * @return the current instance.
     */
    public ChangeRequestMergeDocumentResult setDocumentTitle(String documentTitle)
    {
        this.documentTitle = documentTitle;
        return this;
    }

    /**
     * @return the type of change.
     */
    public FileChange.FileChangeType getType()
    {
        return type;
    }

    /**
     * @return the wrapped {@link MergeDocumentResult} when this change is of type
     *          {@link FileChange.FileChangeType#EDITION}.
     */
    public MergeDocumentResult getWrappedResult()
    {
        return wrappedResult;
    }

    /**
     * @return the identifier of the file change used to compute this merge result.
     * @since 0.6
     */
    public String getFileChangeId()
    {
        return fileChangeId;
    }

    /**
     * Define the reference of the document for which this result is obtained.
     *
     * @param documentReference the reference of the document concerned by this result.
     * @return current instance for builder pattern
     * @since 0.7
     */
    public ChangeRequestMergeDocumentResult setDocumentReference(DocumentReference documentReference)
    {
        this.documentReference = documentReference;
        return this;
    }

    /**
     * @return the reference of the document for which this result is obtained.
     * @since 0.7
     */
    public DocumentReference getDocumentReference()
    {
        return documentReference;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ChangeRequestMergeDocumentResult that = (ChangeRequestMergeDocumentResult) o;

        return new EqualsBuilder()
            .append(isConflictingDeletion, that.isConflictingDeletion)
            .append(type, that.type)
            .append(wrappedResult, that.wrappedResult)
            .append(documentTitle, that.documentTitle)
            .append(fileChangeId, that.fileChangeId)
            .append(documentReference, that.documentReference)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
            .append(type)
            .append(fileChangeId)
            .append(wrappedResult)
            .append(isConflictingDeletion)
            .append(documentTitle)
            .append(documentReference)
            .toHashCode();
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this)
            .append("type", type)
            .append("fileChangeId", fileChangeId)
            .append("wrappedResult", wrappedResult)
            .append("isConflictingDeletion", isConflictingDeletion)
            .append("documentTitle", documentTitle)
            .append("documentReference", documentReference)
            .toString();
    }
}
