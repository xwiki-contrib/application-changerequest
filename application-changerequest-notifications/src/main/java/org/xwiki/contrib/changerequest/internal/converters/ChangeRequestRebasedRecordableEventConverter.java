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
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestRebasedRecordableEvent;

/**
 * Default converter for {@link  ChangeRequestRebasedRecordableEvent}.
 *
 * @version $Id$
 * @since 0.11
 */
@Component
@Singleton
@Named(ChangeRequestRebasedRecordableEvent.EVENT_NAME)
public class ChangeRequestRebasedRecordableEventConverter extends
    AbstractChangeRequestRecordableEventConverter<ChangeRequestRebasedRecordableEvent>
{
    /**
     * Key used for event parameter to store the ID of a filechange in case of rebase.
     */
    public static final String REBASED_FILECHANGE_KEY = CHANGE_REQUEST_PREFIX_PARAMETER_KEY + "rebased.filechange.id";

    /**
     * Key used for event parameter to store if a rebased concerned the whole change request or not.
     */
    public static final String REBASED_ALL_KEY = CHANGE_REQUEST_PREFIX_PARAMETER_KEY + "rebased.all";

    /**
     * Key used for event parameter to store if a rebase event was related to fixing a conflict or not.
     */
    public static final String REBASED_WITH_CONFLICT_FIXING =
        CHANGE_REQUEST_PREFIX_PARAMETER_KEY + "rebased.conflictRelated";

    /**
     * Default constructor.
     */
    public ChangeRequestRebasedRecordableEventConverter()
    {
        super(Collections.singletonList(new ChangeRequestRebasedRecordableEvent()));
    }

    @Override
    protected Map<String, String> getSpecificParameters(ChangeRequestRebasedRecordableEvent event)
    {
        Map<String, String> parameters = new HashMap<>();
        parameters.put(REBASED_FILECHANGE_KEY, event.getConcernedFileChangeId());
        parameters.put(REBASED_ALL_KEY, String.valueOf(event.concernsAllChangeRequest()));
        parameters.put(REBASED_WITH_CONFLICT_FIXING, String.valueOf(event.isConflictFixing()));
        return parameters;
    }
}
