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
package org.xwiki.contrib.changerequest.internal.cache;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.LRUCacheConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.contrib.changerequest.ChangeRequest;

/**
 * Dedicated cache for change request, to avoid having to reload them from xobjects all the time.
 *
 * @version $Id$
 * @since 0.11
 */

@Component(roles = ChangeRequestStorageCacheManager.class)
@Singleton
public class ChangeRequestStorageCacheManager implements Initializable, Disposable
{
    @Inject
    private CacheManager cacheManager;

    private Cache<ChangeRequest> changeRequestCache;

    @Override
    public void initialize() throws InitializationException
    {
        try {
            this.changeRequestCache =
                this.cacheManager.createNewCache(new LRUCacheConfiguration("changerequest.changerequests", 100));
        } catch (CacheException e) {
            throw new InitializationException("Error when initializing the cache for change requests.");
        }
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        this.changeRequestCache.dispose();
    }

    /**
     * Retrieve a change request from the cache with its identifier.
     * @param id the identifier of the change request as used for loading it.
     * @return a {@link Optional#empty()} if the change request is not cached, else an optional containing
     *         the change request.
     */
    public Optional<ChangeRequest> getChangeRequest(String id)
    {
        ChangeRequest changeRequest = this.changeRequestCache.get(id);
        if (changeRequest == null) {
            return Optional.empty();
        } else {
            return Optional.of(changeRequest);
        }
    }

    /**
     * Cache the given change request so that it's quickly loaded later.
     *
     * @param changeRequest the change request to be cached.
     */
    public void cacheChangeRequest(ChangeRequest changeRequest)
    {
        this.changeRequestCache.set(changeRequest.getId(), changeRequest);
    }

    /**
     * Clear the change request value from the cache.
     *
     * @param id the identifier of the change request to be cleared from the cache.
     */
    public void invalidate(String id)
    {
        this.changeRequestCache.remove(id);
    }

    /**
     * Remove all entries from the cache.
     */
    public void invalidateAll()
    {
        this.changeRequestCache.removeAll();
    }
}
