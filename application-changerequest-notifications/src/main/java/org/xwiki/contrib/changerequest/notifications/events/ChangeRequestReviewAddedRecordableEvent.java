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
package org.xwiki.contrib.changerequest.notifications.events;

import org.xwiki.stability.Unstable;

/**
 * Event triggered when a new review has been added to a change request.
 *
 * @version $Id$
 * @since 0.6
 */
@Unstable
public class ChangeRequestReviewAddedRecordableEvent extends AbstractChangeRequestRecordableEvent
{
    /**
     * Event name to be used in the components referring to that event.
     */
    public static final String EVENT_NAME = "changerequest.review.added";

    private final String reviewId;
    private final String originalApprover;

    /**
     * Default empty constructor.
     */
    public ChangeRequestReviewAddedRecordableEvent()
    {
        this(null, null);
    }

    /**
     * Default constructor with a change request id.
     *
     * @param id the identifier of a change request for which the event is triggered.
     * @param reviewId the identifier of the added review related to the event.
     */
    public ChangeRequestReviewAddedRecordableEvent(String id, String reviewId)
    {
        this(id, reviewId, null);
    }

    /**
     * Default constructor with a change request id and an original approver.
     *
     * @param id the identifier of a change request for which the event is triggered.
     * @param reviewId the identifier of the added review related to the event.
     * @param originalApprover the approver on behalf of whom the review is performed: to be used only in case of
     *        delegate approval mechanism.
     * @since 0.13
     */
    @Unstable
    public ChangeRequestReviewAddedRecordableEvent(String id, String reviewId, String originalApprover)
    {
        super(id);
        this.reviewId = reviewId;
        this.originalApprover = originalApprover;
    }

    @Override
    public String getEventName()
    {
        return EVENT_NAME;
    }

    /**
     * @return the id of the added review which led to this event.
     */
    public String getReviewId()
    {
        return reviewId;
    }

    /**
     * @return the original approver in case of a review performed by a delegate, or {@code null}.
     */
    public String getOriginalApprover()
    {
        return originalApprover;
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        return otherEvent instanceof ChangeRequestReviewAddedRecordableEvent;
    }
}
