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

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.notifications.events.AbstractChangeRequestRecordableEvent;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestCreatedRecordableEvent;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestUpdatedRecordableEvent;
import org.xwiki.contrib.changerequest.notifications.events.DocumentModifiedInChangeRequestEvent;
import org.xwiki.contrib.changerequest.notifications.events.StaleChangeRequestRecordableEvent;

/**
 * Default converter to be used for any {@link AbstractChangeRequestRecordableEvent} which don't have any specific
 * parameters.
 *
 * @version $Id$
 * @since 0.11
 */
@Component
@Singleton
@Named("org.xwiki.contrib.changerequest.notifications.events.AbstractChangeRequestRecordableEvent")
public class DefaultChangeRequestRecordableEventConverter extends
    AbstractChangeRequestRecordableEventConverter<AbstractChangeRequestRecordableEvent>
{
    /**
     * Default constructor.
     */
    public DefaultChangeRequestRecordableEventConverter()
    {
        super(Arrays.asList(
            new ChangeRequestCreatedRecordableEvent(),
            new DocumentModifiedInChangeRequestEvent(),
            new ChangeRequestUpdatedRecordableEvent(),
            new StaleChangeRequestRecordableEvent()
        ));
    }

    @Override
    protected Map<String, String> getSpecificParameters(AbstractChangeRequestRecordableEvent event)
    {
        return Collections.emptyMap();
    }
}
