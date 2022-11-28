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
 * Represents a reference related to a specific review.
 *
 * @version $Id$
 * @since 0.6
 */
@Unstable
public class ChangeRequestReviewReference extends AbstractChangeRequestDiscussionContextReference
{
    private final String reviewId;

    /**
     * Default constructor.
     *
     * @param reviewId the identifier of the review
     * @param changeRequestId the identifier of the related change request
     */
    public ChangeRequestReviewReference(String reviewId, String changeRequestId)
    {
        super(changeRequestId, ChangeRequestDiscussionReferenceType.REVIEW, reviewId, true);
        this.reviewId = reviewId;
    }

    /**
     * @return the related review identifier.
     */
    public String getReviewId()
    {
        return reviewId;
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

        ChangeRequestReviewReference that = (ChangeRequestReviewReference) o;

        return new EqualsBuilder().appendSuper(super.equals(o))
            .append(reviewId, that.reviewId).isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37).appendSuper(super.hashCode()).append(reviewId).toHashCode();
    }
}
