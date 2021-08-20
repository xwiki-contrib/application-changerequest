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
package org.xwiki.contrib.changerequest;

import java.util.Date;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.stability.Unstable;
import org.xwiki.user.UserReference;

/**
 * Represents a review of a change request.
 *
 * @version $Id$
 * @since 0.4
 */
@Unstable
public class ChangeRequestReview
{
    private final ChangeRequest changeRequest;
    private final boolean approved;
    private final UserReference author;
    private String comment;
    private Date reviewDate;
    private boolean isSaved;

    /**
     * Default constructor.
     *
     * @param changeRequest the change request for which the review is performed.
     * @param approved either if the change request is approved or not.
     * @param author the author of the review.
     */
    public ChangeRequestReview(ChangeRequest changeRequest, boolean approved, UserReference author)
    {
        this.changeRequest = changeRequest;
        this.approved = approved;
        this.author = author;
        this.reviewDate = new Date();
    }

    /**
     * @return the change request.
     */
    public ChangeRequest getChangeRequest()
    {
        return changeRequest;
    }

    /**
     * @return {@code true} if the change request is approved.
     */
    public boolean isApproved()
    {
        return approved;
    }

    /**
     * @return the author of the review.
     */
    public UserReference getAuthor()
    {
        return author;
    }

    /**
     * @return a comment attached to the review if any.
     */
    public String getComment()
    {
        return comment;
    }

    /**
     * @return the date when the review has been performed.
     */
    public Date getReviewDate()
    {
        return reviewDate;
    }

    /**
     * Attach a comment to the review.
     *
     * @param comment the comment to be attached.
     * @return the current instance.
     */
    public ChangeRequestReview setComment(String comment)
    {
        this.comment = comment;
        return this;
    }

    /**
     * Set the date of the review.
     *
     * @param reviewDate the date when the review has been performed.
     * @return the current instance.
     */
    public ChangeRequestReview setReviewDate(Date reviewDate)
    {
        this.reviewDate = reviewDate;
        return this;
    }

    /**
     * @return {@code true} if this review has been saved already.
     */
    public boolean isSaved()
    {
        return isSaved;
    }

    /**
     * @param saved {@code true} if the review has been saved already.
     * @return the current instance.
     */
    public ChangeRequestReview setSaved(boolean saved)
    {
        isSaved = saved;
        return this;
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

        ChangeRequestReview that = (ChangeRequestReview) o;

        return new EqualsBuilder()
            .append(approved, that.approved)
            .append(author, that.author)
            .append(comment, that.comment)
            .append(reviewDate, that.reviewDate)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
            .append(approved)
            .append(author)
            .append(comment)
            .append(reviewDate)
            .toHashCode();
    }
}
