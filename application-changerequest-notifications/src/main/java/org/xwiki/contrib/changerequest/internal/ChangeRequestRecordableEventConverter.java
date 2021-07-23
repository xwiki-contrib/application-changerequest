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
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestFileChangeAddedRecordableEvent;
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
     * Key used for event parameter containing the change request ID of the event.
     */
    public static final String CHANGE_REQUEST_ID_PARAMETER_KEY = "changerequest.id";

    @Inject
    private RecordableEventConverter defaultConverter;

    @Override
    public Event convert(RecordableEvent recordableEvent, String source, Object data) throws Exception
    {
        Event result = this.defaultConverter.convert(recordableEvent, source, data);
        if (recordableEvent instanceof AbstractChangeRequestRecordableEvent) {
            AbstractChangeRequestRecordableEvent crEvent = (AbstractChangeRequestRecordableEvent) recordableEvent;
            Map<String, String> parameters = new HashMap<>(result.getParameters());
            parameters.put(CHANGE_REQUEST_ID_PARAMETER_KEY, crEvent.getChangeRequestId());
            result.setParameters(parameters);
            result.setType(crEvent.getEventName());
        }
        return result;
    }

    @Override
    public List<RecordableEvent> getSupportedEvents()
    {
        return Arrays.asList(
            new ChangeRequestCreatedRecordableEvent(),
            new ChangeRequestFileChangeAddedRecordableEvent()
        );
    }
}
