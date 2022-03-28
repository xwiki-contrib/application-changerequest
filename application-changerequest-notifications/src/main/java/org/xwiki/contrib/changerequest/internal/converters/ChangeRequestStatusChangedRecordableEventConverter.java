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
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestStatusChangedRecordableEvent;

/**
 * Default converter for {@link  ChangeRequestStatusChangedRecordableEvent}.
 *
 * @version $Id$
 * @since 0.11
 */
@Component
@Singleton
@Named(ChangeRequestStatusChangedRecordableEvent.EVENT_NAME)
public class ChangeRequestStatusChangedRecordableEventConverter extends
    AbstractChangeRequestRecordableEventConverter<ChangeRequestStatusChangedRecordableEvent>
{
    /**
     * Key used for event parameter to store the old status.
     */
    public static final String OLD_STATUS_PARAMETER_KEY = CHANGE_REQUEST_PREFIX_PARAMETER_KEY + "status.old";

    /**
     * Key used for event parameter to store the new status.
     */
    public static final String NEW_STATUS_PARAMETER_KEY = CHANGE_REQUEST_PREFIX_PARAMETER_KEY + "status.new";

    /**
     * Default constructor.
     */
    public ChangeRequestStatusChangedRecordableEventConverter()
    {
        super(Collections.singletonList(new ChangeRequestStatusChangedRecordableEvent()));
    }

    @Override
    protected Map<String, String> getSpecificParameters(ChangeRequestStatusChangedRecordableEvent event)
    {
        Map<String, String> parameters = new HashMap<>();
        parameters.put(OLD_STATUS_PARAMETER_KEY, event.getOldStatus().name());
        parameters.put(NEW_STATUS_PARAMETER_KEY, event.getNewStatus().name());
        return parameters;
    }
}
