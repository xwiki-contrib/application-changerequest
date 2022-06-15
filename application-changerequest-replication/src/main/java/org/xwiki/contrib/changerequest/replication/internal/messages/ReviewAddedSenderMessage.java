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
package org.xwiki.contrib.changerequest.replication.internal.messages;

import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestReviewAddedRecordableEvent;

/**
 * Default sender message implementation for {@link ChangeRequestReviewAddedRecordableEvent}.
 *
 * @version $Id$
 * @since 0.16
 */
@Component
@Named(ChangeRequestReviewAddedRecordableEvent.EVENT_NAME)
public class ReviewAddedSenderMessage extends
    AbstractRecordableChangeRequestEventReplicationSenderMessage<ChangeRequestReviewAddedRecordableEvent>
{
    /**
     * Key of the custom metadata holding the review identifier.
     * @see ChangeRequestReviewAddedRecordableEvent#getReviewId()
     */
    public static final String REVIEW_ID = "REVIEW_ID";

    /**
     * Key of the custom metadata holding the reference of the original approver whenever the review has been performed
     * by a delegate.
     * @see ChangeRequestReviewAddedRecordableEvent#getOriginalApprover()
     */
    public static final String ORIGINAL_APPROVER = "ORIGINAL_APPROVER";

    /**
     * Default constructor.
     */
    public ReviewAddedSenderMessage()
    {
        super(ChangeRequestReviewAddedRecordableEvent.EVENT_NAME);
    }

    @Override
    protected void initializeCustomMetadata(ChangeRequestReviewAddedRecordableEvent event)
    {
        this.putCustomMetadata(REVIEW_ID, event.getReviewId());
        this.putCustomMetadata(ORIGINAL_APPROVER, event.getOriginalApprover());
    }
}
