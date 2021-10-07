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
package org.xwiki.contrib.changerequest.discussions;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.contrib.changerequest.discussions.references.AbstractChangeRequestDiscussionContextReference;
import org.xwiki.contrib.discussions.domain.Discussion;
import org.xwiki.stability.Unstable;

/**
 * Represents a discussion for change request, which contains both the discussion object and the reference to link it
 * to a change request element.
 *
 * @version $Id$
 * @since 0.6
 */
@Unstable
public class ChangeRequestDiscussion
{
    private final AbstractChangeRequestDiscussionContextReference reference;
    private final Discussion discussion;

    /**
     * Default constructor.
     *
     * @param reference the reference to link the discussion to a change request element
     * @param discussion the actual discussion
     */
    public ChangeRequestDiscussion(AbstractChangeRequestDiscussionContextReference reference, Discussion discussion)
    {
        this.reference = reference;
        this.discussion = discussion;
    }

    /**
     * @return the reference to link the discussion to a change request element
     */
    public AbstractChangeRequestDiscussionContextReference getReference()
    {
        return reference;
    }

    /**
     * @return the actual discussion
     */
    public Discussion getDiscussion()
    {
        return discussion;
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

        ChangeRequestDiscussion that = (ChangeRequestDiscussion) o;

        return new EqualsBuilder().append(reference, that.reference)
            .append(discussion, that.discussion).isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37).append(reference).append(discussion).toHashCode();
    }
}
