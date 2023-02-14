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
package org.xwiki.contrib.changerequest.internal.listeners;

import java.util.Collections;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.events.ChangeRequestFileChangeAddedEvent;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestFileChangeAddedRecordableEvent;
import org.xwiki.contrib.changerequest.notifications.events.DocumentModifiedInChangeRequestEvent;
import org.xwiki.observation.event.Event;

/**
 * Listener in charge of handling {@link ChangeRequestFileChangeAddedEvent}.
 *
 * @version $Id$
 * @since 0.6
 */
@Component
@Named(ChangeRequestFileChangeAddedEventListener.NAME)
@Singleton
public class ChangeRequestFileChangeAddedEventListener extends AbstractChangeRequestEventListener
{
    static final String NAME = "ChangeRequestFileChangeAddedEventListener";

    /**
     * Default constructor.
     */
    public ChangeRequestFileChangeAddedEventListener()
    {
        super(NAME, Collections.singletonList(new ChangeRequestFileChangeAddedEvent()));
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        String changeRequestId = (String) source;
        try {
            // We send two events in case of file change added:
            // one that targets the watchers of the document that has been modified
            // the other one that targets the watchers of the change request itself.
            FileChange fileChange = (FileChange) data;
            this.titleCacheManagerProvider.get().invalidate(changeRequestId, fileChange);
            DocumentModelBridge documentInstance = this.documentAccessBridge.getTranslatedDocumentInstance(
                fileChange.getTargetEntity());
            this.notifyChangeRequestRecordableEvent(new DocumentModifiedInChangeRequestEvent(changeRequestId),
                documentInstance);
            documentInstance = this.getChangeRequestDocument(changeRequestId);
            ChangeRequestFileChangeAddedRecordableEvent recordableEvent =
                new ChangeRequestFileChangeAddedRecordableEvent(changeRequestId, fileChange.getId());
            this.notifyChangeRequestRecordableEvent(recordableEvent, documentInstance);
        } catch (Exception e) {
            this.logger.error(
                "Error while getting the document instance from [{}] after a filechange added event: [{}]",
                data, ExceptionUtils.getRootCauseMessage(e)
            );
        }
    }
}
