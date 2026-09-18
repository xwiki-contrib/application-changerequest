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
 * <p>
 * Loading a change request is not atomic: the data is read from the documents, the instance is built, and only then
 * handed over to this cache. An invalidation occurring in between concerns data that the instance being built has
 * already read, so storing it would serve that outdated state to every later request until the next invalidation.
 * Each identifier therefore carries a generation, incremented by every invalidation: a loader reads it through
 * {@link #startLoading(String)} before reading the documents and hands it back to
 * {@link #cacheChangeRequest(ChangeRequest, long)}, which only stores the change request when the generation is
 * unchanged.
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

    private Cache<CacheEntry> changeRequestCache;

    /**
     * An entry of the cache: either a cached change request, or a tombstone left behind by an invalidation. Both
     * carry the generation of their identifier, which is what allows an outdated load to be detected. A tombstone is
     * kept instead of removing the entry so that a load started before an invalidation can see that it happened.
     */
    private static final class CacheEntry
    {
        private final long generation;

        private final ChangeRequest changeRequest;

        CacheEntry(long generation, ChangeRequest changeRequest)
        {
            this.generation = generation;
            this.changeRequest = changeRequest;
        }
    }

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
        CacheEntry entry = this.changeRequestCache.get(id);
        if (entry == null || entry.changeRequest == null) {
            return Optional.empty();
        } else {
            return Optional.of(entry.changeRequest);
        }
    }

    /**
     * Declare that the change request with the given identifier is about to be loaded, and return the generation to
     * hand back to {@link #cacheChangeRequest(ChangeRequest, long)} once it is loaded. This must be called before
     * reading the data the change request is built from, otherwise an invalidation concurrent with that reading
     * cannot be detected.
     *
     * @param id the identifier of the change request about to be loaded.
     * @return the current generation of that identifier.
     * @since 1.24
     */
    public synchronized long startLoading(String id)
    {
        CacheEntry entry = this.changeRequestCache.get(id);
        if (entry == null) {
            // The entry is created right away so that its later absence, be it from an eviction or from
            // invalidateAll, is enough to tell that the generation cannot be trusted anymore.
            entry = new CacheEntry(0, null);
            this.changeRequestCache.set(id, entry);
        }
        return entry.generation;
    }

    /**
     * Cache the given change request so that it's quickly loaded later, unless it has been invalidated since the
     * given generation was obtained from {@link #startLoading(String)}, in which case it is dropped: it has been
     * built from data that is already outdated.
     *
     * @param changeRequest the change request to be cached.
     * @param generation the generation obtained from {@link #startLoading(String)} before loading it.
     * @since 1.24
     */
    public synchronized void cacheChangeRequest(ChangeRequest changeRequest, long generation)
    {
        String id = changeRequest.getId();
        CacheEntry entry = this.changeRequestCache.get(id);
        if (entry != null && entry.generation == generation) {
            this.changeRequestCache.set(id, new CacheEntry(generation, changeRequest));
        }
    }

    /**
     * Clear the change request value from the cache.
     *
     * @param id the identifier of the change request to be cleared from the cache.
     */
    public synchronized void invalidate(String id)
    {
        CacheEntry entry = this.changeRequestCache.get(id);
        long generation = (entry == null) ? 1 : entry.generation + 1;
        this.changeRequestCache.set(id, new CacheEntry(generation, null));
    }

    /**
     * Remove all entries from the cache.
     */
    public synchronized void invalidateAll()
    {
        this.changeRequestCache.removeAll();
    }
}
