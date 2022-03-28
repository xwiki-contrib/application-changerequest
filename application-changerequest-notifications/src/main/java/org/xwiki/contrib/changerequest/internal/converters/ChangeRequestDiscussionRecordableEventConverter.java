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
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestDiscussionRecordableEvent;

/**
 * Default converter for {@link  ChangeRequestDiscussionRecordableEvent}.
 *
 * @version $Id$
 * @since 0.11
 */
@Component
@Singleton
@Named(ChangeRequestDiscussionRecordableEvent.EVENT_NAME)
public class ChangeRequestDiscussionRecordableEventConverter extends
    AbstractChangeRequestRecordableEventConverter<ChangeRequestDiscussionRecordableEvent>
{
    /**
     * Key used for event parameter to store a discussion type.
     */
    public static final String DISCUSSION_TYPE_KEY = CHANGE_REQUEST_PREFIX_PARAMETER_KEY + "discussion.type";

    /**
     * Key used for event parameter to store a discussion reference.
     */
    public static final String DISCUSSION_REFERENCE_KEY = CHANGE_REQUEST_PREFIX_PARAMETER_KEY + "discussion.reference";

    /**
     * Key used for event parameter to store a message reference.
     */
    public static final String MESSAGE_REFERENCE_KEY = CHANGE_REQUEST_PREFIX_PARAMETER_KEY + "message.reference";

    /**
     * Default constructor.
     */
    public ChangeRequestDiscussionRecordableEventConverter()
    {
        super(Collections.singletonList(new ChangeRequestDiscussionRecordableEvent()));
    }

    @Override
    protected Map<String, String> getSpecificParameters(ChangeRequestDiscussionRecordableEvent event)
    {
        Map<String, String> parameters = new HashMap<>();
        parameters.put(DISCUSSION_TYPE_KEY, event.getDiscussionType());
        parameters.put(DISCUSSION_REFERENCE_KEY, event.getDiscussionReference());
        parameters.put(MESSAGE_REFERENCE_KEY, event.getMessageReference());
        return parameters;
    }
}
