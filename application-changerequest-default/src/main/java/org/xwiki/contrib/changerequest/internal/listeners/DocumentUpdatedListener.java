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

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestManager;
import org.xwiki.contrib.changerequest.internal.cache.MergeCacheManager;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.job.Job;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.event.AbstractLocalEventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Listener in charge of checking if the updates performed on documents doesn't modify the merging status of related
 * change requests.
 *
 * @version $Id$
 * @since 0.6
 */
@Component
@Singleton
@Named(DocumentUpdatedListener.NAME)
public class DocumentUpdatedListener extends AbstractLocalEventListener
{
    static final String NAME = "org.xwiki.contrib.changerequest.internal.DocumentUpdatedListener";

    private static final List<Event> EVENT_LIST = Arrays.asList(
        new DocumentCreatedEvent(),
        new DocumentUpdatedEvent(),
        new DocumentDeletedEvent()
    );

    @Inject
    private Provider<ChangeRequestStorageManager> storageManager;

    @Inject
    private Provider<ChangeRequestManager> changeRequestManager;

    @Inject
    private Provider<MergeCacheManager> conflictCacheManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private Logger logger;

    /**
     * Default constructor.
     */
    public DocumentUpdatedListener()
    {
        super(NAME, EVENT_LIST);
    }

    @Override
    public void processLocalEvent(Event event, Object source, Object data)
    {
        XWikiDocument sourceDoc = (XWikiDocument) source;
        DocumentReference reference = sourceDoc.getDocumentReferenceWithLocale();

        // We ignore all updates occurring during a wiki initialization.
        if (isWikiReady(reference.getWikiReference())) {
            try {
                List<ChangeRequest> changeRequests = this.storageManager.get().findChangeRequestTargeting(reference);
                for (ChangeRequest changeRequest : changeRequests) {
                    if (changeRequest.getStatus().isOpen()) {
                        this.changeRequestManager.get().computeReadyForMergingStatus(changeRequest);
                    }
                }
            } catch (ChangeRequestException e) {
                logger.warn("Error while computing the merging status of change requests after update of [{}]: [{}]",
                    reference, ExceptionUtils.getRootCauseMessage(e));
            }
        }
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        super.onEvent(event, source, data);

        // The cache invalidation should be performed even in case of remote event.
        XWikiDocument sourceDoc = (XWikiDocument) source;
        DocumentReference reference = sourceDoc.getDocumentReferenceWithLocale();
        if (isWikiReady(reference.getWikiReference())) {
            this.conflictCacheManager.get().invalidate(reference);
        }
    }

    private boolean isWikiReady(WikiReference wikiReference)
    {
        XWikiContext context = this.contextProvider.get();
        boolean result = true;
        if (!context.getMainXWiki().equals(wikiReference.getName())) {
            Job wikiInitializerJob = context.getWiki().getWikiInitializerJob(wikiReference.getName());
            result = wikiInitializerJob != null
                && wikiInitializerJob.getStatus().getState() == JobStatus.State.FINISHED;
        }
        return result;
    }
}
