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
package org.xwiki.contrib.changerequest.internal.converters;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestReviewAddedRecordableEvent;

/**
 * Default converter for {@link  ChangeRequestReviewAddedRecordableEvent}.
 *
 * @version $Id$
 * @since 0.11
 */
@Component
@Singleton
@Named(ChangeRequestReviewAddedRecordableEvent.EVENT_NAME)
public class ChangeRequestReviewAddedRecordableEventConverter extends
    AbstractChangeRequestRecordableEventConverter<ChangeRequestReviewAddedRecordableEvent>
{
    /**
     * Key used for event parameter to store the review ID.
     */
    public static final String REVIEW_ID_PARAMETER_KEY = CHANGE_REQUEST_PREFIX_PARAMETER_KEY + "review.id";

    /**
     * Key used to retrieve the original approver in case of delegate approval.
     * @since 0.13
     */
    public static final String ORIGINAL_APPROVER_PARAMETER_KEY = CHANGE_REQUEST_PREFIX_PARAMETER_KEY
        + "review.originalApprover";

    /**
     * Default constructor.
     */
    public ChangeRequestReviewAddedRecordableEventConverter()
    {
        super(Collections.singletonList(new ChangeRequestReviewAddedRecordableEvent()));
    }

    @Override
    protected Map<String, String> getSpecificParameters(ChangeRequestReviewAddedRecordableEvent event)
    {
        Map<String, String> parameterMap = new HashMap<>();
        parameterMap.put(REVIEW_ID_PARAMETER_KEY, event.getReviewId());

        if (event.getOriginalApprover() != null) {
            parameterMap.put(ORIGINAL_APPROVER_PARAMETER_KEY, event.getOriginalApprover());
        }
        return parameterMap;
    }
}
