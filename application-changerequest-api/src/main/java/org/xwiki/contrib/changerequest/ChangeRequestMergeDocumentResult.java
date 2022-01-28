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

import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
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
    private final FileChange fileChange;
    private MergeDocumentResult wrappedResult;
    private final boolean isConflicting;
    private String documentTitle;
    private final String previousVersion;
    private final Date previousVersionDate;

    private ChangeRequestMergeDocumentResult(FileChange fileChange, boolean isConflicting,
        String previousVersion, Date previousVersionDate)
    {
        this.fileChange = fileChange;
        this.isConflicting = isConflicting;
        this.previousVersion = previousVersion;
        this.previousVersionDate = previousVersionDate;
    }

    /**
     * Constructor to use in case of {@link FileChange.FileChangeType#EDITION}.
     *
     * @param mergeDocumentResult the result of the merge that will be wrapped
     * @param fileChange the file change used for computing this merge
     * @param previousVersion the previous version of the document used to perform the merge
     * @param previousVersionDate the date of the version of the document used to perform the merge
     */
    public ChangeRequestMergeDocumentResult(MergeDocumentResult mergeDocumentResult, FileChange fileChange,
        String previousVersion, Date previousVersionDate)
    {
        this(fileChange, mergeDocumentResult.hasConflicts(), previousVersion, previousVersionDate);
        this.wrappedResult = mergeDocumentResult;
    }

    /**
     * Constructor to use in case of {@link FileChange.FileChangeType#DELETION} or
     * {@link FileChange.FileChangeType#CREATION}.
     *
     * @param isConflicting {@code true} if there's a conflict with this change.
     * @param fileChange the file change used for computing this merge
     * @param previousVersion the current version of the document used to perform the merge
     * @param previousVersionDate the date of the version of the document used to perform the merge
     */
    public ChangeRequestMergeDocumentResult(boolean isConflicting, FileChange fileChange,
        String previousVersion, Date previousVersionDate)
    {
        this(fileChange, isConflicting, previousVersion, previousVersionDate);
        if (fileChange.getType() != FileChange.FileChangeType.DELETION
            && fileChange.getType() != FileChange.FileChangeType.CREATION) {
            throw new IllegalArgumentException(
                "This constructor should only be used for deletion or creation file changes.");
        }
    }


    /**
     * Check if this result exposes a conflict.
     * @return {@code true} if the wrapped result contains a conflict, or in case of deletion if a conflict
     *          was computed.
     */
    public boolean hasConflicts()
    {
        return this.isConflicting;
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
        return fileChange.getType();
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
     * @return the file change concerned for this merge.
     * @since 0.7
     */
    public FileChange getFileChange()
    {
        return fileChange;
    }

    /**
     * @return a unique identifier for that merge.
     */
    public String getIdentifier()
    {
        return StringUtils.replace(String.format("%s_%s_%s", fileChange.getVersion(),
            previousVersion, previousVersionDate.getTime()), ".", "dot");
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
            .append(isConflicting, that.isConflicting)
            .append(fileChange, that.fileChange)
            .append(wrappedResult, that.wrappedResult)
            .append(documentTitle, that.documentTitle)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
            .append(fileChange)
            .append(wrappedResult)
            .append(isConflicting)
            .append(documentTitle)
            .toHashCode();
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this)
            .append("wrappedResult", wrappedResult)
            .append("isConflicting", isConflicting)
            .append("documentTitle", documentTitle)
            .append("fileChange", fileChange)
            .toString();
    }
}
