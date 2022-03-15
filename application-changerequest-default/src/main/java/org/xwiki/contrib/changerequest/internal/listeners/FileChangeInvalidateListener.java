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
import org.xwiki.contrib.changerequest.internal.MergeCacheManager;
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
@Named(FileChangeInvalidateListener.NAME)
public class FileChangeInvalidateListener extends AbstractEventListener
{
    static final String NAME = "org.xwiki.contrib.changerequest.internal.listeners.FileChangeInvalidateListener";

    @Inject
    private Provider<MergeCacheManager> mergeCacheManager;

    /**
     * Default constructor.
     */
    public FileChangeInvalidateListener()
    {
        super(NAME, new ChangeRequestUpdatedFileChangeEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (data instanceof FileChange) {
            this.mergeCacheManager.get().invalidate((FileChange) data);
        } else if (data instanceof ChangeRequest) {
            ((ChangeRequest) data).getLastFileChanges()
                .forEach(fileChange -> this.mergeCacheManager.get().invalidate(fileChange));
        }
    }
}
