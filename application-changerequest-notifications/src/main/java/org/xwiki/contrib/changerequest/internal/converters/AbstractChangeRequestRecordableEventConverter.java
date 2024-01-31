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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.xwiki.contrib.changerequest.notifications.events.AbstractChangeRequestRecordableEvent;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.RecordableEvent;
import org.xwiki.eventstream.RecordableEventConverter;

/**
 * Abstract component for converting {@link AbstractChangeRequestRecordableEvent}.
 * For using it without any parameters, see {@link DefaultChangeRequestRecordableEventConverter}.
 *
 * @param <T> the specific type of {@link AbstractChangeRequestRecordableEvent} to convert.
 *
 * @version $Id$
 * @since 0.11
 */
public abstract class AbstractChangeRequestRecordableEventConverter<T extends AbstractChangeRequestRecordableEvent>
    implements RecordableEventConverter
{
    /**
     * Global prefix to be used for any parameters in events related to change requests.
     */
    public static final String CHANGE_REQUEST_PREFIX_PARAMETER_KEY = "changerequest.";

    /**
     * Key used for event parameter to store the change request ID.
     */
    public static final String CHANGE_REQUEST_ID_PARAMETER_KEY = CHANGE_REQUEST_PREFIX_PARAMETER_KEY + "id";

    private final List<RecordableEvent> supportedEvents;

    @Inject
    private RecordableEventConverter defaultConverter;

    protected AbstractChangeRequestRecordableEventConverter(List<RecordableEvent> supportedEvents)
    {
        this.supportedEvents = supportedEvents;
    }

    @Override
    public Event convert(RecordableEvent recordableEvent, String source, Object data) throws Exception
    {
        Event result = this.defaultConverter.convert(recordableEvent, source, data);
        Map<String, Object> parameters = new HashMap<>(result.getCustom());

        T crEvent = (T) recordableEvent;
        parameters.put(CHANGE_REQUEST_ID_PARAMETER_KEY, crEvent.getChangeRequestId());
        result.setType(crEvent.getEventName());

        // We put a specific groupId to avoid having events grouped with other events performed during same request
        // this might happen in particular when a review is performed leading to the change of a change request
        // status, or when a page is updated which also leads to a change of change request status.
        result.setGroupId(crEvent.getEventName());
        parameters.putAll(this.getSpecificParameters(crEvent));

        result.setCustom(parameters);
        return result;
    }

    @Override
    public List<RecordableEvent> getSupportedEvents()
    {
        return this.supportedEvents;
    }

    protected abstract Map<String, String> getSpecificParameters(T event);
}
