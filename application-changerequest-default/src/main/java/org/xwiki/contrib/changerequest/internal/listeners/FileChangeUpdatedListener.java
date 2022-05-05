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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestManager;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.events.ChangeRequestUpdatedFileChangeEvent;
import org.xwiki.contrib.changerequest.internal.cache.MergeCacheManager;
import org.xwiki.contrib.changerequest.internal.cache.ChangeRequestStorageCacheManager;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.RemoteObservationManagerContext;

/**
 * Listener called whenever a filechange has been updated in a change request.
 * This listener performs different actions:
 * <li>
 *     <ul>it invalidates the merge cache manager</ul>
 *     <ul>it invalidates the approvals reviews</ul>
 *     <ul>it compute back the ready for merging status</ul>
 * </li>
 *
 * Component dedicated to invalidate the merge cache manager entries whenever a filechange is updated.
 *
 * @version $Id$
 * @since 0.11
 */
@Component
@Singleton
@Named(FileChangeUpdatedListener.NAME)
public class FileChangeUpdatedListener extends AbstractEventListener
{
    static final String NAME = "org.xwiki.contrib.changerequest.internal.listeners.FileChangeCacheInvalidationListener";

    @Inject
    private Provider<MergeCacheManager> mergeCacheManager;

    @Inject
    private Provider<ChangeRequestStorageCacheManager> changeRequestCacheManager;

    @Inject
    private Provider<ChangeRequestManager> changeRequestManagerProvider;

    @Inject
    private RemoteObservationManagerContext remoteObservationManagerContext;

    @Inject
    private Logger logger;

    /**
     * Default constructor.
     */
    public FileChangeUpdatedListener()
    {
        super(NAME, new ChangeRequestUpdatedFileChangeEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        String changeRequestId = (String) source;
        ChangeRequest changeRequest = null;
        // We perform those changes even for remote event since we want the cache to be invalidated in such case.
        if (data instanceof FileChange) {
            FileChange fileChange = (FileChange) data;
            this.mergeCacheManager.get().invalidate(fileChange);
            this.changeRequestCacheManager.get().invalidate(changeRequestId);
            changeRequest = fileChange.getChangeRequest();
        } else if (data instanceof ChangeRequest) {
            changeRequest = (ChangeRequest) data;
            changeRequest.getLastFileChanges()
                .forEach(fileChange -> this.mergeCacheManager.get().invalidate(fileChange));
            this.changeRequestCacheManager.get().invalidate(changeRequestId);
        }
        // This only needs to be perform for local events.
        if (changeRequest != null && !this.remoteObservationManagerContext.isRemoteState()) {
            this.computeStatus(changeRequest);
            this.invalidateApprovals(changeRequest);
        }
    }

    private void computeStatus(ChangeRequest changeRequest)
    {
        try {
            this.changeRequestManagerProvider.get().computeReadyForMergingStatus(changeRequest);
        } catch (ChangeRequestException e) {
            this.logger.error("Error while computing ready for merging status of [{}]", changeRequest, e);
        }
    }

    private void invalidateApprovals(ChangeRequest changeRequest)
    {
        try {
            this.changeRequestManagerProvider.get().invalidateReviews(changeRequest);
        } catch (ChangeRequestException e) {
            this.logger.error("Error while invalidating reviews of [{}]", changeRequest, e);
        }
    }
}
