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
package org.xwiki.contrib.changerequest.discussions.references.difflocation;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.xwiki.stability.Unstable;

/**
 * Represents the location of a specific line of a specific diff.
 *
 * @version $Id$
 * @since 0.7
 */
@Unstable
public class LineDiffLocation extends AbstractDiffLocation
{
    /**
     * Represents the different part of a document that are displayed in a diff.
     */
    public enum DiffDocumentPart
    {
        /**
         * Any xobject property change of the document: when used the entity reference should be the reference of
         * the object, and the diffBlockId should be the name of the property.
         */
        XOBJECT,

        /**
         * Any xclass property change of the document: when used the entity reference should be the reference of
         * the xclass, and the diffBlockId should be the name of the property.
         */
        XCLASS,

        /**
         * Any metadata of the document: it can be the content, the title, the author, the date etc. When used,
         * the entity reference can be either {@code _} to represent the reference contained in the file diff location,
         * or the explicit reference. The diffBlockId should be the name of the property.
         */
        METADATA
    }

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
    private final DiffDocumentPart documentPart;
    private final String entityReference;
    private final String diffBlockId;
    private final LineChange lineChange;
    private final FileDiffLocation fileDiffLocation;

    /**
     * Default constructor.
     *
     * @param fileDiffLocation the file diff in which this line appears.
     * @param documentPart the part of the document concerned by this location.
     * @param entityReference the reference of the modified entity referenced.
     * @param diffBlockId the name of the concerned property.
     * @param lineNumber the line number concerned.
     * @param lineChange define if the line number is among the added, removed or context lines.
     */
    public LineDiffLocation(FileDiffLocation fileDiffLocation, DiffDocumentPart documentPart,
        String entityReference, String diffBlockId, long lineNumber, LineChange lineChange)
    {
        this.fileDiffLocation = fileDiffLocation;
        this.documentPart = documentPart;
        this.entityReference = entityReference;
        this.diffBlockId = diffBlockId;
        this.lineChange = lineChange;
        this.lineNumber = lineNumber;
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
    public DiffDocumentPart getDocumentPart()
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

    /**
     * @return the specific location in the diff.
     */
    public String getDiffBlockId()
    {
        return diffBlockId;
    }

    /**
     * @return the reference of the specific entity referred to here.
     */
    public String getEntityReference()
    {
        return entityReference;
    }

    /**
     * @return the actual file diff where this line diff appears.
     */
    public FileDiffLocation getFileDiffLocation()
    {
        return fileDiffLocation;
    }

    /**
     * Parse the given reference to create a new instance of {@link LineDiffLocation}.
     * Note that if the reference cannot be parsed, it will throw a {@link IllegalArgumentException}.
     *
     * @param reference the reference to be parsed.
     * @return a new instance of {@link LineDiffLocation} with the information contained in the reference.
     * @throws IllegalArgumentException if the reference is not using the right format.
     */
    public static LineDiffLocation parse(String reference)
    {
        List<String> stringList = LineDiffLocation.parseToList(reference);
        // We should obtain 7 tokens:
        // 2 first for FileDiffLocation
        // 5 then for remaining fields of LineDiffLocation

        if (stringList.size() != 7) {
            throw new IllegalArgumentException(
                String.format("Error when parsing [%s] to line diff location.", reference));
        } else {
            FileDiffLocation fileDiffLocation = FileDiffLocation.parseList(stringList);
            DiffDocumentPart documentPart = DiffDocumentPart.valueOf(stringList.get(2));
            String entityReference = stringList.get(3);
            String diffBlockId = stringList.get(4);
            LineChange lineChange = LineChange.valueOf(stringList.get(5));
            long lineNumber = Long.parseLong(stringList.get(6));
            return new LineDiffLocation(fileDiffLocation, documentPart, entityReference, diffBlockId,
                lineNumber, lineChange);
        }
    }

    /**
     * Serialize the reference so that it can be later parsed.
     * Note that this method does not use the same format as {@link #toString()} so this one should only be used for
     * debug purposes.
     *
     * @return a string containing the information of the location and ready to be parsed.
     */
    public String getSerializedReference()
    {
        String serializedString = this.getSerializedString(Arrays.asList(
            this.documentPart.name(),
            this.entityReference,
            this.diffBlockId,
            this.lineChange.name(),
            String.valueOf(this.lineNumber)));
        return this.fileDiffLocation.getSerializedReference() + SEPARATOR + serializedString;
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

        LineDiffLocation that = (LineDiffLocation) o;

        return new EqualsBuilder()
            .append(lineNumber, that.lineNumber)
            .append(documentPart, that.documentPart)
            .append(entityReference, that.entityReference)
            .append(diffBlockId, that.diffBlockId)
            .append(lineChange, that.lineChange)
            .append(fileDiffLocation, that.fileDiffLocation).isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(63, 37)
            .append(lineNumber)
            .append(documentPart)
            .append(entityReference)
            .append(diffBlockId)
            .append(lineChange)
            .append(fileDiffLocation)
            .toHashCode();
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this)
            .append("lineNumber", lineNumber)
            .append("documentPart", documentPart)
            .append("entityReference", entityReference)
            .append("diffBlockId", diffBlockId)
            .append("lineChange", lineChange)
            .append("fileDiffLocation", fileDiffLocation)
            .toString();
    }
}
