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

import java.util.Collections;

import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestCreatedRecordableEvent;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestFileChangeAddedRecordableEvent;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.store.internal.AbstractLegacyEventConverter;
import org.xwiki.eventstream.store.internal.LegacyEvent;

import static
    org.xwiki.contrib.changerequest.internal.ChangeRequestRecordableEventConverter.CHANGE_REQUEST_ID_PARAMETER_KEY;

/**
 * We need this custom legacy converter to ensure that the change request id parameters from the
 * {@link org.xwiki.eventstream.internal.DefaultEvent} are set in the parameter3 of the {@link LegacyEvent}: this is
 * what will be stored.
 * FIXME: We should actually check if it's still needed with Solr store?
 *
 * @version $Id$
 * @since 0.3
 */
// FIXME: we should have a better management of type once XWIKI-17156 is fixed.
@Component(hints = {
    ChangeRequestCreatedRecordableEvent.EVENT_NAME,
    ChangeRequestFileChangeAddedRecordableEvent.EVENT_NAME
})
@Singleton
public class ChangeRequestLegacyEventConverter extends AbstractLegacyEventConverter
{
    @Override
    public LegacyEvent convertEventToLegacyActivity(Event e)
    {
        LegacyEvent event = super.convertEventToLegacyActivity(e);
        if (e.getParameters().containsKey(CHANGE_REQUEST_ID_PARAMETER_KEY)) {
            event.setParam3(e.getParameters().get(CHANGE_REQUEST_ID_PARAMETER_KEY));
        }
        return event;
    }

    @Override
    public Event convertLegacyActivityToEvent(LegacyEvent e)
    {
        Event event = super.convertLegacyActivityToEvent(e);
        if (!StringUtils.isEmpty(e.getParam3())) {
            event.setParameters(Collections.singletonMap(CHANGE_REQUEST_ID_PARAMETER_KEY, e.getParam3()));
        }
        return event;
    }
}
