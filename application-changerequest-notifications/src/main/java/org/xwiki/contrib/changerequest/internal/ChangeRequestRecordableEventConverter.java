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
package org.xwiki.contrib.changerequest.internal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.notifications.events.AbstractChangeRequestRecordableEvent;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestCreatedRecordableEvent;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestDiscussionRecordableEvent;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestFileChangeAddedRecordableEvent;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestReviewAddedRecordableEvent;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestStatusChangedRecordableEvent;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestUpdatedRecordableEvent;
import org.xwiki.contrib.changerequest.notifications.events.DocumentModifiedInChangeRequestEvent;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.RecordableEvent;
import org.xwiki.eventstream.RecordableEventConverter;

/**
 * Converter component used to inject the needed parameters for displaying the notifications.
 *
 * @version $Id$
 * @since 0.3
 */
@Component
@Singleton
@Named("changerequest")
public class ChangeRequestRecordableEventConverter implements RecordableEventConverter
{
    /**
     * Global prefix to be used for any parameters in events related to change requests.
     */
    public static final String CHANGE_REQUEST_PREFIX_PARAMETER_KEY = "changerequest.";

    /**
     * Key used for event parameter to store the change request ID.
     */
    public static final String CHANGE_REQUEST_ID_PARAMETER_KEY = CHANGE_REQUEST_PREFIX_PARAMETER_KEY + "id";

    /**
     * Key used for event parameter to store the file change ID.
     */
    public static final String FILECHANGE_ID_PARAMETER_KEY = CHANGE_REQUEST_PREFIX_PARAMETER_KEY + "filechange.id";

    /**
     * Key used for event parameter to store the review ID.
     */
    public static final String REVIEW_ID_PARAMETER_KEY = CHANGE_REQUEST_PREFIX_PARAMETER_KEY + "review.id";

    /**
     * Key used for event parameter to store the old status.
     */
    public static final String OLD_STATUS_PARAMETER_KEY = CHANGE_REQUEST_PREFIX_PARAMETER_KEY + "status.old";

    /**
     * Key used for event parameter to store the new status.
     */
    public static final String NEW_STATUS_PARAMETER_KEY = CHANGE_REQUEST_PREFIX_PARAMETER_KEY + "status.new";

    /**
     * Key used for event parameter to store a discussion type.
     */
    public static final String DISCUSSION_TYPE_KEY = CHANGE_REQUEST_PREFIX_PARAMETER_KEY + "discussion.type";

    /**
     * Key used for event parameter to store a discussion reference.
     */
    public static final String DISCUSSION_REFERENCE_KEY = CHANGE_REQUEST_PREFIX_PARAMETER_KEY + "discussion.reference";

    @Inject
    private RecordableEventConverter defaultConverter;

    @Override
    public Event convert(RecordableEvent recordableEvent, String source, Object data) throws Exception
    {
        Event result = this.defaultConverter.convert(recordableEvent, source, data);
        Map<String, String> parameters = new HashMap<>(result.getParameters());
        if (recordableEvent instanceof AbstractChangeRequestRecordableEvent) {
            AbstractChangeRequestRecordableEvent crEvent = (AbstractChangeRequestRecordableEvent) recordableEvent;
            parameters.put(CHANGE_REQUEST_ID_PARAMETER_KEY, crEvent.getChangeRequestId());
            result.setType(crEvent.getEventName());

            // We put a specific groupId to avoid having events grouped with other events performed during same request
            // this might happen in particular when a review is performed leading to the change of a change request
            // status, or when a page is updated which also leads to a change of change request status.
            result.setGroupId(crEvent.getEventName());
        }
        if (recordableEvent instanceof ChangeRequestFileChangeAddedRecordableEvent) {
            ChangeRequestFileChangeAddedRecordableEvent crEvent =
                (ChangeRequestFileChangeAddedRecordableEvent) recordableEvent;
            parameters.put(FILECHANGE_ID_PARAMETER_KEY, crEvent.getFileChangeId());
        }
        if (recordableEvent instanceof ChangeRequestReviewAddedRecordableEvent) {
            ChangeRequestReviewAddedRecordableEvent crEvent = (ChangeRequestReviewAddedRecordableEvent) recordableEvent;
            parameters.put(REVIEW_ID_PARAMETER_KEY, crEvent.getReviewId());
        }
        if (recordableEvent instanceof ChangeRequestStatusChangedRecordableEvent) {
            ChangeRequestStatusChangedRecordableEvent crEvent =
                (ChangeRequestStatusChangedRecordableEvent) recordableEvent;
            parameters.put(OLD_STATUS_PARAMETER_KEY, crEvent.getOldStatus().name());
            parameters.put(NEW_STATUS_PARAMETER_KEY, crEvent.getNewStatus().name());
        }
        if (recordableEvent instanceof ChangeRequestDiscussionRecordableEvent) {
            ChangeRequestDiscussionRecordableEvent discussionRecordableEvent =
                (ChangeRequestDiscussionRecordableEvent) recordableEvent;
            parameters.put(DISCUSSION_TYPE_KEY, discussionRecordableEvent.getDiscussionType());
            parameters.put(DISCUSSION_REFERENCE_KEY, discussionRecordableEvent.getDiscussionReference());
        }

        result.setParameters(parameters);
        return result;
    }

    @Override
    public List<RecordableEvent> getSupportedEvents()
    {
        return Arrays.asList(
            new ChangeRequestCreatedRecordableEvent(),
            new ChangeRequestFileChangeAddedRecordableEvent(),
            new ChangeRequestReviewAddedRecordableEvent(),
            new ChangeRequestStatusChangedRecordableEvent(),
            new DocumentModifiedInChangeRequestEvent(),
            new ChangeRequestDiscussionRecordableEvent(),
            new ChangeRequestUpdatedRecordableEvent()
        );
    }
}
