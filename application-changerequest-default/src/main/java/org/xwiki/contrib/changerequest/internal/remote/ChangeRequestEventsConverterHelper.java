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
package org.xwiki.contrib.changerequest.internal.remote;

import java.io.Serializable;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.internal.cache.ChangeRequestStorageCacheManager;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.observation.remote.RemoteEventData;

/**
 * Helper component for {@link ChangeRequestEventsConverter} operations.
 *
 * @version $Id$
 * @since 1.5
 */
@Component(roles = ChangeRequestEventsConverterHelper.class)
@Singleton
public class ChangeRequestEventsConverterHelper
{
    @Inject
    private ChangeRequestStorageCacheManager changeRequestCacheManager;

    @Inject
    private ChangeRequestStorageManager changeRequestStorageManager;

    /**
     * Retrieve a change request based on the given information or throw a
     * {@link ChangeRequestEventsConverterException}. Note that this method also invalidate the change request from
     * the cache, so that the new values are loaded.
     *
     * @param changeRequestId the identifier of the change request to load.
     * @param remoteEvent the event which triggered that call.
     * @return the change request instance with the matching identifier.
     * @throws ChangeRequestEventsConverterException if the change request cannot be found or if there is a problem
     *                                               when loading it
     */
    public ChangeRequest getChangeRequest(String changeRequestId, RemoteEventData remoteEvent)
        throws ChangeRequestEventsConverterException
    {
        // We'll need to reload the change request from DB so first invalidate the cache value for it.
        this.changeRequestCacheManager.invalidate(changeRequestId);

        try {
            Optional<ChangeRequest> optionalChangeRequest =
                this.changeRequestStorageManager.load(changeRequestId);
            if (optionalChangeRequest.isPresent()) {
                return optionalChangeRequest.get();
            } else {
                throw new ChangeRequestEventsConverterException(
                    String.format("Cannot find change request [%s] to convert event [%s]", changeRequestId,
                        remoteEvent));
            }
        } catch (ChangeRequestException e) {
            throw new ChangeRequestEventsConverterException(
               String.format("Error when loading change request [%s] to convert event [%s]",
                changeRequestId, remoteEvent), e);
        }
    }

    /**
     * Load a filechange based on the given information or throw a {@link ChangeRequestEventsConverterException}.
     *
     * @param changeRequestId the identifier of the change request where to find the file change
     * @param fileChangeId the identifier of the file change to load
     * @param remoteEvent the event which triggered that call
     * @return the filechange instance matching the information
     * @throws ChangeRequestEventsConverterException if the change request or the file change cannot be found, or in
     *                                               case of problem when loading the change request.
     */
    public FileChange getFileChange(String changeRequestId, Serializable fileChangeId, RemoteEventData remoteEvent)
        throws ChangeRequestEventsConverterException
    {
        ChangeRequest changeRequest = this.getChangeRequest(changeRequestId, remoteEvent);
        Optional<FileChange> optionalFileChange = changeRequest.getFileChangeById((String) fileChangeId);
        if (optionalFileChange.isPresent()) {
            return optionalFileChange.get();
        } else {
            throw new ChangeRequestEventsConverterException(
                String.format("Cannot find file change [%s] from change request [%s] to convert event [%s].",
                    fileChangeId, changeRequestId, remoteEvent));
        }
    }
}
