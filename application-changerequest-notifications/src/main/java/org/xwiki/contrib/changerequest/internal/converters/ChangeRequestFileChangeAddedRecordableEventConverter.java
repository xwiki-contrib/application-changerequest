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
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestFileChangeAddedRecordableEvent;

/**
 * Default converter for {@link  ChangeRequestFileChangeAddedRecordableEvent}.
 *
 * @version $Id$
 * @since 0.11
 */
@Component
@Singleton
@Named(ChangeRequestFileChangeAddedRecordableEvent.EVENT_NAME)
public class ChangeRequestFileChangeAddedRecordableEventConverter extends
    AbstractChangeRequestRecordableEventConverter<ChangeRequestFileChangeAddedRecordableEvent>
{
    /**
     * Key used for event parameter to store the file change ID.
     */
    public static final String FILECHANGE_ID_PARAMETER_KEY = CHANGE_REQUEST_PREFIX_PARAMETER_KEY + "filechange.id";

    /**
     * Default constructor.
     */
    public ChangeRequestFileChangeAddedRecordableEventConverter()
    {
        super(Collections.singletonList(new ChangeRequestFileChangeAddedRecordableEvent()));
    }

    @Override
    protected Map<String, String> getSpecificParameters(ChangeRequestFileChangeAddedRecordableEvent event)
    {
        return Collections.singletonMap(FILECHANGE_ID_PARAMETER_KEY, event.getFileChangeId());
    }
}
