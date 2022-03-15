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

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.events.ChangeRequestUpdatedFileChangeEvent;
import org.xwiki.contrib.changerequest.internal.cache.MergeCacheManager;
import org.xwiki.contrib.changerequest.internal.cache.ChangeRequestStorageCacheManager;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

/**
 * Component dedicated to invalidate the merge cache manager entries whenever a filechange is updated.
 *
 * @version $Id$
 * @since 0.11
 */
@Component
@Singleton
@Named(FileChangeCacheInvalidationListener.NAME)
public class FileChangeCacheInvalidationListener extends AbstractEventListener
{
    static final String NAME = "org.xwiki.contrib.changerequest.internal.listeners.FileChangeCacheInvalidationListener";

    @Inject
    private Provider<MergeCacheManager> mergeCacheManager;

    @Inject
    private Provider<ChangeRequestStorageCacheManager> changeRequestCacheManager;

    /**
     * Default constructor.
     */
    public FileChangeCacheInvalidationListener()
    {
        super(NAME, new ChangeRequestUpdatedFileChangeEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        String changeRequestId = (String) source;
        if (data instanceof FileChange) {
            FileChange fileChange = (FileChange) data;
            this.mergeCacheManager.get().invalidate(fileChange);
            this.changeRequestCacheManager.get().invalidate(changeRequestId);
        } else if (data instanceof ChangeRequest) {
            ChangeRequest changeRequest = (ChangeRequest) data;
            changeRequest.getLastFileChanges()
                .forEach(fileChange -> this.mergeCacheManager.get().invalidate(fileChange));
            this.changeRequestCacheManager.get().invalidate(changeRequestId);
        }
    }
}
