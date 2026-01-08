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

import java.util.HashMap;
import java.util.Map;
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
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.diff.HtmlDiffResult;
import org.xwiki.model.reference.DocumentReference;

/**
 * Cache manager for holding the rendered diff.
 *
 * @version $Id$
 * @since 1.3
 */
@Component(roles = DiffCacheManager.class)
@Singleton
public class DiffCacheManager implements Initializable, Disposable
{
    @Inject
    private CacheManager cacheManager;

    private Cache<Map<DocumentReference, HtmlDiffResult>> renderedDiffCache;

    @Override
    public void initialize() throws InitializationException
    {
        try {
            this.renderedDiffCache =
                this.cacheManager.createNewCache(new LRUCacheConfiguration("changerequest.renderedDiff", 100));
        } catch (CacheException e) {
            throw new InitializationException("Error while creating cache", e);
        }
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        this.renderedDiffCache.dispose();
    }

    /**
     * Retrieve the rendered diff kept in cache if it exists.
     *
     * @param fileChange the file change for which to retrieve the rendered diff.
     * @return an {@link Optional#empty()} if there's no diff in cache else the computed rendered diff.
     */
    public Optional<HtmlDiffResult> getRenderedDiff(FileChange fileChange)
    {
        Optional<HtmlDiffResult> result = Optional.empty();
        String changeRequestId = fileChange.getChangeRequest().getId();
        Map<DocumentReference, HtmlDiffResult> map = this.renderedDiffCache.get(changeRequestId);

        if (map != null && map.containsKey(fileChange.getTargetEntity())) {
            result = Optional.of(map.get(fileChange.getTargetEntity()));
        }
        return result;
    }

    /**
     * Record in cache the rendered diff.
     *
     * @param fileChange the file change for which the diff has been computed.
     * @param renderedDiff the computed diff.
     */
    public void setRenderedDiff(FileChange fileChange, HtmlDiffResult renderedDiff)
    {
        String changeRequestId = fileChange.getChangeRequest().getId();
        Map<DocumentReference, HtmlDiffResult> map = this.renderedDiffCache.get(changeRequestId);
        if (map == null) {
            map = new HashMap<>();
            this.renderedDiffCache.set(changeRequestId, map);
        }
        map.put(fileChange.getTargetEntity(), renderedDiff);
    }

    /**
     * Invalidate the cache for the given change request.
     *
     * @param changeRequest the change request for which data should be invalidated.
     */
    public void invalidate(ChangeRequest changeRequest)
    {
        this.renderedDiffCache.remove(changeRequest.getId());
    }

    /**
     * Invalidate the cache for the given file change.
     *
     * @param fileChange the change for which data should be invalidated.
     */
    public void invalidate(FileChange fileChange)
    {
        String changeRequestId = fileChange.getChangeRequest().getId();
        Map<DocumentReference, HtmlDiffResult> map = this.renderedDiffCache.get(changeRequestId);
        if (map != null) {
            map.remove(fileChange.getTargetEntity());
        }
    }

    /**
     * Invalidate entirely the cache.
     * @since 1.5
     * @since 1.4.4
     */
    public void invalidateAll()
    {
        this.renderedDiffCache.removeAll();
    }
}
