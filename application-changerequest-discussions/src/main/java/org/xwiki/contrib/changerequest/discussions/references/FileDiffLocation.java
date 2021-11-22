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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.xwiki.stability.Unstable;

/**
 * Representation of a location of a file diff, used both in {@link ChangeRequestFileDiffReference} and
 * {@link ChangeRequestLineDiffReference}.
 *
 * @version $Id$
 * @since 0.7
 */
@Unstable
public class FileDiffLocation
{
    private static final String TARGET_REFERENCE = "targetReference";
    private static final String FILECHANGE_ID = "fileChangeId";
    private static final String DIFF_ID = "diffId";

    private static final Pattern REFERENCE_PATTERN = Pattern.compile(
        String.format("^(?<%s>.+)"
            + "_(?<%s>.+)"
            + "_(?<%s>\\w+)$", TARGET_REFERENCE, FILECHANGE_ID, DIFF_ID));

    private final String fileChangeId;
    private final String diffId;
    private final String targetReference;

    /**
     * Default constructor.
     *
     * @param fileChangeId the identifier of the file change used for the diff.
     * @param diffId the unique identifier of the diff itself.
     * @param targetReference the reference of the document for which a diff is displayed.
     */
    public FileDiffLocation(String fileChangeId, String diffId, String targetReference)
    {
        this.fileChangeId = fileChangeId;
        this.diffId = diffId;
        this.targetReference = targetReference;
    }

    /**
     * @return the identifier of the file change involved in the diff
     */
    public String getFileChangeId()
    {
        return fileChangeId;
    }

    /**
     * @return the identifier of the diff the reference is related to
     */
    public String getDiffId()
    {
        return diffId;
    }

    /**
     * @return the serialized reference of the document involved in the diff
     */
    public String getTargetReference()
    {
        return targetReference;
    }

    /**
     * Verify if the given reference can be parsed.
     *
     * @see #parse(String)
     * @param reference a reference without the change request identifier part.
     * @return {@code true} only if the given reference can be parsed.
     */
    public static boolean isMatching(String reference)
    {
        return REFERENCE_PATTERN.matcher(reference).matches();
    }

    /**
     * Serialized the location to be used as a reference: this serialization can always be parsed back.
     * Note that the {@link #toString()} method does not use same format and should only be used for debug purpose.
     *
     * @return a serialization of the location.
     */
    public String getSerializedReference()
    {
        return String.format("%s_%s_%s", targetReference, fileChangeId, diffId);
    }

    /**
     * Parse the given reference and build a corresponding instance of {@link FileDiffLocation}.
     * Note that if the given reference cannot be parsed, it throws an {@link IllegalArgumentException}.
     *
     * @see #isMatching(String)
     * @param reference the reference to be parsed.
     * @return a new instance of {@link FileDiffLocation}
     * @throws IllegalArgumentException if the reference cannot be parsed
     */
    public static FileDiffLocation parse(String reference)
    {
        Matcher matcher = REFERENCE_PATTERN.matcher(reference);
        if (matcher.matches()) {
            String targetReference = matcher.group(TARGET_REFERENCE);
            String fileChangeId = matcher.group(FILECHANGE_ID);
            String diffId = matcher.group(DIFF_ID);
            return new FileDiffLocation(fileChangeId, diffId, targetReference);
        } else {
            throw new IllegalArgumentException(String.format("[%s] is not matching the reference pattern.", reference));
        }
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

        FileDiffLocation that = (FileDiffLocation) o;

        return new EqualsBuilder().append(fileChangeId, that.fileChangeId)
            .append(diffId, that.diffId).append(targetReference, that.targetReference).isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37).append(fileChangeId).append(diffId).append(targetReference).toHashCode();
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this)
            .append(FILECHANGE_ID, fileChangeId)
            .append(DIFF_ID, diffId)
            .append(TARGET_REFERENCE, targetReference)
            .toString();
    }
}
