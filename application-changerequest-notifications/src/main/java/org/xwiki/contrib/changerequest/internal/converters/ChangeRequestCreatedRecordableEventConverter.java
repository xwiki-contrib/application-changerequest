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
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestCreatedRecordableEvent;

/**
 * Converter for {@link ChangeRequestCreatedRecordableEvent}.
 *
 * @version $Id$
 * @since 0.14
 */
@Component
@Singleton
@Named(ChangeRequestCreatedRecordableEvent.EVENT_NAME)
public class ChangeRequestCreatedRecordableEventConverter extends
    AbstractChangeRequestRecordableEventConverter<ChangeRequestCreatedRecordableEvent>
{
    /**
     * Parameter for {@link ChangeRequestCreatedRecordableEvent#isFromSplit()} value.
     */
    public static final String IS_FROM_SPLIT_PARAMETER = CHANGE_REQUEST_PREFIX_PARAMETER_KEY + "create.fromSplit";

    /**
     * Default constructor.
     */
    public ChangeRequestCreatedRecordableEventConverter()
    {
        super(Collections.singletonList(new ChangeRequestCreatedRecordableEvent()));
    }

    @Override
    protected Map<String, String> getSpecificParameters(ChangeRequestCreatedRecordableEvent event)
    {
        Map<String, String> result = new HashMap<>();
        result.put(IS_FROM_SPLIT_PARAMETER, Boolean.toString(event.isFromSplit()));
        result.put(ChangeRequestFileChangeAddedRecordableEventConverter.FILECHANGE_ID_PARAMETER_KEY,
            event.getFileChangeId());
        return result;
    }
}
