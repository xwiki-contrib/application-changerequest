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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
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
public class LineDiffLocation
{
    private static final String FILE_DIFF_LOCATION = "fileDiffLocation";
    private static final String DOCUMENT_PART = "documentPart";
    private static final String DOCUMENT_PART_LOCATION = "documentPartLocation";
    private static final String LINE_CHANGE = "lineChange";
    private static final String LINE_NUMBER = "lineNumber";

    /**
     * Pattern used to extract the different information of the actual reference serialized in
     * {@link #getSerializedReference()}.
     * Pattern output:
     * ^(?<fileDiffLocation>.+)_(?<documentPart>(CONTENT|XOBJECT|XCLASS|METADATA))_
     * (?<documentPartLocation>.+)_(?<lineChange>(ADDED|REMOVED|UNCHANGED))_(?<lineNumber>\d+)$
     */
    private static final Pattern REFERENCE_PATTERN = Pattern.compile(
        String.format("^(?<%s>.+)_(?<%s>(%s))_(?<%s>.+)_(?<%s>(%s))_(?<%s>\\d+)$", FILE_DIFF_LOCATION,
            DOCUMENT_PART, StringUtils.join(DiffDocumentPart.values(), '|'),
            DOCUMENT_PART_LOCATION,
            LINE_CHANGE, StringUtils.join(LineChange.values(), '|'),
            LINE_NUMBER));

    /**
     * Represents the different part of a document that are displayed in a diff.
     */
    public enum DiffDocumentPart
    {
        /**
         * The actual content change of the document.
         * Note that when it's used, the document part location should be also {@code content} by convention.
         */
        CONTENT,

        /**
         * Any xobject change of the document: when used the document part location should be the full reference of
         * the xobject property modified.
         */
        XOBJECT,

        /**
         * Any xclass change of the document: when used the document part location should be the name of the property
         * modified.
         */
        XCLASS,

        /**
         * Any other metadata of the document: it can be the title, the author, the date etc. When used, the document
         * part location should be the actual name of the property.
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
    private final String documentPartLocation;
    private final LineChange lineChange;
    private final FileDiffLocation fileDiffLocation;

    /**
     * Default constructor.
     *
     * @param fileDiffLocation the file diff in which this line appears.
     * @param documentPart the part of the document concerned by this location.
     * @param documentPartLocation specify the document part (see {@link DiffDocumentPart} for details)
     * @param lineNumber the line number concerned.
     * @param lineChange define if the line number is among the added, removed or context lines.
     */
    public LineDiffLocation(FileDiffLocation fileDiffLocation, DiffDocumentPart documentPart,
        String documentPartLocation, long lineNumber, LineChange lineChange)
    {
        this.fileDiffLocation = fileDiffLocation;
        this.documentPart = documentPart;
        this.documentPartLocation = documentPartLocation;
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
    public String getDocumentPartLocation()
    {
        return documentPartLocation;
    }

    /**
     * @return the actual file diff where this line diff appears.
     */
    public FileDiffLocation getFileDiffLocation()
    {
        return fileDiffLocation;
    }

    /**
     * Verify if the given reference can be parsed to create a {@link LineDiffLocation}.
     *
     * @see #parse(String)
     * @param reference the reference for which to check if it can be parsed
     * @return {@code true} only if the given reference can be parsed
     */
    public static boolean isMatching(String reference)
    {
        return REFERENCE_PATTERN.matcher(reference).matches();
    }

    /**
     * Parse the given reference to create a new instance of {@link LineDiffLocation}.
     * Note that if the reference cannot be parsed, it will throw a {@link IllegalArgumentException}.
     *
     * @see #isMatching(String)
     * @param reference the reference to be parsed.
     * @return a new instance of {@link LineDiffLocation} with the information contained in the reference.
     * @throws IllegalArgumentException if the reference is not using the right format.
     */
    public static LineDiffLocation parse(String reference)
    {
        Matcher matcher = REFERENCE_PATTERN.matcher(reference);
        if (matcher.matches()) {
            FileDiffLocation fileDiffLocation = FileDiffLocation.parse(matcher.group(FILE_DIFF_LOCATION));
            DiffDocumentPart documentPart = DiffDocumentPart.valueOf(matcher.group(DOCUMENT_PART));
            String documentPartLocation = matcher.group(DOCUMENT_PART_LOCATION);
            long lineNumber = Long.parseLong(matcher.group(LINE_NUMBER));
            LineChange lineChange = LineChange.valueOf(matcher.group(LINE_CHANGE));
            return new LineDiffLocation(fileDiffLocation, documentPart, documentPartLocation, lineNumber, lineChange);
        } else {
            throw new IllegalArgumentException(
                    String.format("[%s] cannot be parsed to a line diff reference.", reference));
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
        return String.format("%s_%s_%s_%s_%s",
            this.fileDiffLocation.getSerializedReference(),
            this.documentPart.name(),
            this.documentPartLocation,
            this.lineChange.name(),
            this.lineNumber);
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

        return new EqualsBuilder().append(lineNumber, that.lineNumber)
            .append(documentPart, that.documentPart).append(documentPartLocation, that.documentPartLocation)
            .append(lineChange, that.lineChange).append(fileDiffLocation, that.fileDiffLocation).isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37).append(lineNumber).append(documentPart).append(documentPartLocation)
            .append(lineChange).append(fileDiffLocation).toHashCode();
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this)
            .append(FILE_DIFF_LOCATION, fileDiffLocation)
            .append(DOCUMENT_PART, documentPart)
            .append(DOCUMENT_PART_LOCATION, documentPartLocation)
            .append(LINE_CHANGE, lineChange)
            .append(LINE_NUMBER, lineNumber)
            .toString();
    }
}
