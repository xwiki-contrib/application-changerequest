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
import org.xwiki.contrib.changerequest.discussions.references.ChangeRequestFileDiffReference;
import org.xwiki.contrib.changerequest.discussions.references.ChangeRequestLineDiffReference;
import org.xwiki.stability.Unstable;

/**
 * Representation of a location of a file diff, used both in {@link ChangeRequestFileDiffReference} and
 * {@link ChangeRequestLineDiffReference}.
 *
 * @version $Id$
 * @since 0.7
 */
@Unstable
public class FileDiffLocation extends AbstractDiffLocation
{
    private final String diffId;
    private final String targetReference;

    /**
     * Default constructor.
     *
     * @param diffId the unique identifier of the diff itself.
     * @param targetReference the reference of the document for which a diff is displayed.
     */
    public FileDiffLocation(String diffId, String targetReference)
    {
        this.diffId = diffId;
        this.targetReference = targetReference;
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
     * Serialized the location to be used as a reference: this serialization can always be parsed back.
     * Note that the {@link #toString()} method does not use same format and should only be used for debug purpose.
     *
     * @return a serialization of the location.
     */
    public String getSerializedReference()
    {
        return this.getSerializedString(Arrays.asList(this.targetReference, this.diffId));
    }

    protected static FileDiffLocation parseList(List<String> tokens)
    {
        return new FileDiffLocation(tokens.get(1), tokens.get(0));
    }

    /**
     * Parse the given reference and build a corresponding instance of {@link FileDiffLocation}.
     * Note that if the given reference cannot be parsed, it throws an {@link IllegalArgumentException}.
     *
     * @param reference the reference to be parsed.
     * @return a new instance of {@link FileDiffLocation}
     * @throws IllegalArgumentException if the reference cannot be parsed
     */
    public static FileDiffLocation parse(String reference)
    {
        List<String> tokens = FileDiffLocation.parseToList(reference);
        if (tokens.size() != 2) {
            throw new IllegalArgumentException(
                String.format("Error when parsing [%s] to file diff location reference", reference));
        } else {
            return FileDiffLocation.parseList(tokens);
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

        return new EqualsBuilder()
            .append(diffId, that.diffId)
            .append(targetReference, that.targetReference)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
            .append(diffId)
            .append(targetReference)
            .toHashCode();
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this)
            .append("diffId", diffId)
            .append("targetReference", targetReference)
            .toString();
    }
}
