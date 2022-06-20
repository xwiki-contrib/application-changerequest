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

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.events.ChangeRequestUpdatingFileChangeEvent;
import org.xwiki.contrib.changerequest.internal.cache.ChangeRequestStorageCacheManager;
import org.xwiki.contrib.changerequest.internal.cache.MergeCacheManager;
import org.xwiki.contrib.changerequest.internal.storage.FileChangeXClassInitializer;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.model.reference.RegexEntityReference;
import org.xwiki.observation.ObservationContext;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XObjectAddedEvent;
import com.xpn.xwiki.internal.event.XObjectDeletedEvent;
import com.xpn.xwiki.internal.event.XObjectUpdatedEvent;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;

/**
 * Listener in charge of invalidating some caches whenever the filechange xobjects are directly updated.
 * Note that this listener is called only if we're not inside a {@link ChangeRequestUpdatingFileChangeEvent}.
 *
 * @version $Id$
 * @since 0.14
 */
@Component
@Singleton
@Named(FileChangeXObjectUpdatedListener.NAME)
public class FileChangeXObjectUpdatedListener extends AbstractLocalEventListener
{
    static final String NAME = "org.xwiki.contrib.changerequest.internal.listeners.FileChangeXObjectUpdatedListener";

    static final RegexEntityReference REFERENCE =
        BaseObjectReference.any(FileChangeXClassInitializer.FILECHANGE_XCLASS.toString());

    static final List<Event> EVENT_LIST = List.of(
        new XObjectAddedEvent(REFERENCE),
        new XObjectUpdatedEvent(REFERENCE),
        new XObjectDeletedEvent(REFERENCE)
    );

    @Inject
    private ObservationContext observationContext;

    @Inject
    private Provider<ChangeRequestStorageCacheManager> storageCacheManagerProvider;

    @Inject
    private Provider<ChangeRequestStorageManager> storageManagerProvider;

    @Inject
    private Provider<MergeCacheManager> mergeCacheManagerProvider;

    @Inject
    private Logger logger;

    /**
     * Default constructor.
     */
    public FileChangeXObjectUpdatedListener()
    {
        super(NAME, EVENT_LIST);
    }

    @Override
    public void processLocalEvent(Event event, Object source, Object data)
    {
        if (!observationContext.isIn(new ChangeRequestUpdatingFileChangeEvent())) {
            XWikiDocument updatedDoc = (XWikiDocument) source;
            if (event instanceof XObjectDeletedEvent) {
                updatedDoc = updatedDoc.getOriginalDocument();
            }
            BaseObject xObject = updatedDoc.getXObject(FileChangeXClassInitializer.FILECHANGE_XCLASS);
            String changeRequestId = xObject.getStringValue(FileChangeXClassInitializer.CHANGE_REQUEST_ID);
            // We load the change request before invalidating the cache, so that we can get the filechanges in cache
            // even if a filechange xobject has been deleted, and we ensure to clean some references that should be.
            try {
                Optional<ChangeRequest> changeRequestOpt = this.storageManagerProvider.get().load(changeRequestId);
                if (changeRequestOpt.isPresent()) {
                    ChangeRequest changeRequest = changeRequestOpt.get();
                    changeRequest.getAllFileChanges()
                        .forEach(fileChange -> this.mergeCacheManagerProvider.get().invalidate(fileChange));
                } else {
                    logger.warn("Change request [{}] not found", changeRequestId);
                }
            } catch (ChangeRequestException e) {
                logger.error("Error while loading change request [{}]", changeRequestId, e);
            }

            this.storageCacheManagerProvider.get().invalidate(changeRequestId);

        }
    }
}
