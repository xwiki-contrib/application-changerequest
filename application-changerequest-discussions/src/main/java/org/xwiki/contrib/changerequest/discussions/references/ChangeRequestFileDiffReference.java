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
    protected final String fileChangeId;

    /**
     * Protected constructor to allow inheritance.
     *
     * @param fileChangeId the identifier of the file change involved in the diff
     * @param changeRequestId the identifier of the related change request
     * @param type the actual type of reference
     * @param reference the concrete reference to be used
     */
    protected ChangeRequestFileDiffReference(String fileChangeId, String changeRequestId,
        ChangeRequestDiscussionReferenceType type, String reference)
    {
        super(changeRequestId, type, reference);
        this.fileChangeId = fileChangeId;
    }

    /**
     * Default constructor.
     *
     * @param fileChangeId the identifier of the file change involved in the diff
     * @param changeRequestId the identifier of the related change request
     */
    public ChangeRequestFileDiffReference(String fileChangeId, String changeRequestId)
    {
        this(fileChangeId, changeRequestId, ChangeRequestDiscussionReferenceType.FILE_DIFF, fileChangeId);
    }

    /**
     * @return the identifier of the file change involved in the diff
     */
    public String getFileChangeId()
    {
        return fileChangeId;
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
            .append(fileChangeId, that.fileChangeId).isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37).appendSuper(super.hashCode()).append(fileChangeId).toHashCode();
    }
}
