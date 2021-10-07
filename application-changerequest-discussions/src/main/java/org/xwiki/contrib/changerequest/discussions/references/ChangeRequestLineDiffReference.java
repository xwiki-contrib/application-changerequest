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
package org.xwiki.contrib.changerequest.discussions.references;

import java.util.regex.Pattern;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.stability.Unstable;
import org.xwiki.store.merge.MergeDocumentResult;

/**
 * Represents a reference attached to a particular line of a file diff.
 * A line of a file diff is uniquely identified with a set of 5 elements:
 * <ul>
 *     <li>the identifier of the change request where the diff is computed</li>
 *     <li>the identifier of the file change used for computing the diff</li>
 *     <li>the part of the diff where to find the line (e.g. content of the document, properties, xobjects, etc)</li>
 *     <li>the actual line number of the given document part</li>
 *     <li>the type of change (addition, deletion or context) since the same line number could refer to different lines
 *     in a diff</li>
 * </ul>
 *
 * @version $Id$
 * @since 0.6
 */
@Unstable
public class ChangeRequestLineDiffReference extends ChangeRequestFileDiffReference
{
    /**
     * Pattern used to extract the different information of the actual reference retrieved from {@link #getReference()}.
     */
    public static final Pattern REFERENCE_PATTERN = Pattern.compile(
        "^(?<fileChangeId>.+)_(?<documentPart>\\w+)_(?<lineNumber>\\d+)_(?<lineChange>(ADDED|REMOVED|UNCHANGED))$");

    /**
     * The type of change a line diff is displaying.
     */
    public enum LineChange
    {
        /**
         * When a new line has been added.
         */
        ADDED,

        /**
         * When a line has been removed.
         */
        REMOVED,

        /**
         * When a line is displayed for the context.
         */
        UNCHANGED
    }

    private final long lineNumber;
    private final MergeDocumentResult.DocumentPart documentPart;
    private final LineChange lineChange;

    /**
     * Default constructor.
     *
     * @param fileChangeId identifier of the file change involved in the diff
     * @param changeRequestId identifier of the related change request
     * @param documentPart part of the document diff where the line is located
     * @param lineNumber number of the line in the diff
     * @param lineChange type of change the line number refers to
     */
    public ChangeRequestLineDiffReference(String fileChangeId, String changeRequestId,
        MergeDocumentResult.DocumentPart documentPart, long lineNumber, LineChange lineChange)
    {
        super(fileChangeId, changeRequestId, ChangeRequestDiscussionReferenceType.LINE_DIFF,
            String.format("%s_%s_%s_%s", fileChangeId, documentPart, lineNumber, lineChange.name()));

        this.lineNumber = lineNumber;
        this.documentPart = documentPart;
        this.lineChange = lineChange;
    }

    /**
     * @return the actual line number in the diff.
     */
    public long getLineNumber()
    {
        return this.lineNumber;
    }

    /**
     * @return the diff document part the line belongs to.
     */
    public MergeDocumentResult.DocumentPart getDocumentPart()
    {
        return this.documentPart;
    }

    /**
     * @return the type of change the line refers to.
     */
    public LineChange getLineChange()
    {
        return lineChange;
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

        ChangeRequestLineDiffReference that = (ChangeRequestLineDiffReference) o;

        return new EqualsBuilder().appendSuper(super.equals(o))
            .append(lineNumber, that.lineNumber).append(documentPart, that.documentPart)
            .append(lineChange, that.lineChange)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37).appendSuper(super.hashCode()).append(lineNumber).append(documentPart)
            .append(lineChange).toHashCode();
    }
}
