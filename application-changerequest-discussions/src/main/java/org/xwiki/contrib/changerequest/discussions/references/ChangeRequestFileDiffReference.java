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
import org.xwiki.contrib.changerequest.discussions.references.difflocation.FileDiffLocation;
import org.xwiki.stability.Unstable;

/**
 * Represents a reference related to a specific file of the diff, without specifying the actual line.
 *
 * @version $Id$
 * @since 0.6
 */
@Unstable
public class ChangeRequestFileDiffReference extends AbstractChangeRequestDiscussionContextReference
{
    private final FileDiffLocation fileDiffLocation;

    /**
     * Protected constructor to allow inheritance.
     *
     * @param changeRequestId the identifier of the related change request
     * @param fileDiffLocation the actual file diff location for this reference
     */
    public ChangeRequestFileDiffReference(String changeRequestId, FileDiffLocation fileDiffLocation)
    {
        super(changeRequestId, ChangeRequestDiscussionReferenceType.FILE_DIFF,
            fileDiffLocation.getSerializedReference(), false);
        this.fileDiffLocation = fileDiffLocation;
    }

    /**
     * @return the actual location of the reference.
     */
    public FileDiffLocation getFileDiffLocation()
    {
        return fileDiffLocation;
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

        ChangeRequestFileDiffReference that = (ChangeRequestFileDiffReference) o;

        return new EqualsBuilder().appendSuper(super.equals(o))
            .append(fileDiffLocation, that.fileDiffLocation).isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37).appendSuper(super.hashCode()).append(fileDiffLocation).toHashCode();
    }
}
