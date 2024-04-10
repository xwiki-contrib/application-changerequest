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

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.internal.converters.AbstractChangeRequestRecordableEventConverter;
import org.xwiki.eventstream.Event;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;

/**
 * Abstract class for commons rendering operation of notification for both email and alert rendering.
 *
 * @version $Id$
 * @since 1.14
 */
public abstract class AbstractChangeRequestNotificationRenderer
{
    protected static final String CHANGE_REQUEST_REFERENCES_BINDING_NAME = "changeRequestReferences";

    @Inject
    private DocumentReferenceResolver<ChangeRequest> changeRequestDocumentReferenceResolver;

    protected Map<Event, DocumentReference> getChangeRequestReferences(CompositeEvent compositeEvent)
        throws NotificationException
    {
        Map<Event, DocumentReference> result = new HashMap<>();
        for (Event event : compositeEvent.getEvents()) {
            Map<String, Object> parameters = event.getCustom();
            if (parameters.containsKey(AbstractChangeRequestRecordableEventConverter.CHANGE_REQUEST_ID_PARAMETER_KEY)) {
                String crId = (String) parameters
                    .get(AbstractChangeRequestRecordableEventConverter.CHANGE_REQUEST_ID_PARAMETER_KEY);
                // We don't load the change request on purpose here:
                // we still want it to be resolved even if it has been deleted, so that the notifications are displayed.
                ChangeRequest changeRequest = new ChangeRequest();
                changeRequest.setId(crId);
                DocumentReference changeRequestDocReference =
                    this.changeRequestDocumentReferenceResolver.resolve(changeRequest);
                result.put(event, changeRequestDocReference);
            } else {
                throw new NotificationException(
                    String.format("The event [%s] did not have the appropriate parameter to retrieve "
                        + "the change request.", event.getId()));
            }
        }
        return result;
    }
}
