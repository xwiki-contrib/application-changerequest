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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.contrib.changerequest.discussions.references.difflocation.LineDiffLocation;
import org.xwiki.stability.Unstable;

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
public class ChangeRequestLineDiffReference extends AbstractChangeRequestDiscussionContextReference
{
    private final LineDiffLocation lineDiffLocation;

    /**
     * Default constructor.
     *
     * @param changeRequestId identifier of the related change request
     * @param lineDiffLocation the actual exact location of the line diff
     */
    public ChangeRequestLineDiffReference(String changeRequestId, LineDiffLocation lineDiffLocation)
    {
        super(changeRequestId, ChangeRequestDiscussionReferenceType.LINE_DIFF,
            lineDiffLocation.getSerializedReference());
        this.lineDiffLocation = lineDiffLocation;
    }

    /**
     * @return the actual location of the reference.
     */
    public LineDiffLocation getLineDiffLocation()
    {
        return lineDiffLocation;
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
            .append(lineDiffLocation, that.lineDiffLocation).isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37).appendSuper(super.hashCode()).append(lineDiffLocation).toHashCode();
    }
}
