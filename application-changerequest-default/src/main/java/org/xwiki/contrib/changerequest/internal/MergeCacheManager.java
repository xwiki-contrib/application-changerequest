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
package org.xwiki.contrib.changerequest.internal;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.tuple.Pair;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.LRUCacheConfiguration;
import org.xwiki.cache.event.CacheEntryEvent;
import org.xwiki.cache.event.CacheEntryListener;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.contrib.changerequest.ChangeRequestMergeDocumentResult;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

/**
 * Internal component aiming at keeping a cache of conflict status of filechanges.
 *
 * @version $Id$
 * @since 0.11
 */
@Component(roles = MergeCacheManager.class)
@Singleton
public class MergeCacheManager implements Initializable, Disposable
{
    @Inject
    private CacheManager cacheManager;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    private Cache<Pair<DocumentReference, Boolean>> hasConflictCache;
    private Cache<ChangeRequestMergeDocumentResult> crMergeDocumentResultCache;

    private final Map<DocumentReference, Set<String>> cacheKeysMap = new ConcurrentHashMap<>();

    private final class ConflictCacheEntryListener implements CacheEntryListener<Pair<DocumentReference, Boolean>>
    {
        @Override
        public void cacheEntryAdded(CacheEntryEvent<Pair<DocumentReference, Boolean>> event)
        {
            DocumentReference documentReference = event.getEntry().getValue().getLeft();
            String key = event.getEntry().getKey();
            MergeCacheManager.this.addCacheEntry(documentReference, key);
        }

        @Override
        public void cacheEntryRemoved(CacheEntryEvent<Pair<DocumentReference, Boolean>> event)
        {
            DocumentReference documentReference = event.getEntry().getValue().getLeft();
            String key = event.getEntry().getKey();
            MergeCacheManager.this.removeCacheEntry(documentReference, key);
        }

        @Override
        public void cacheEntryModified(CacheEntryEvent<Pair<DocumentReference, Boolean>> event)
        {
            // Nothing to do.
        }
    }

    private final class CRMergeDocumentResultCacheEntryListener
        implements CacheEntryListener<ChangeRequestMergeDocumentResult>
    {
        @Override
        public void cacheEntryAdded(CacheEntryEvent<ChangeRequestMergeDocumentResult> event)
        {
            DocumentReference documentReference = event.getEntry().getValue().getFileChange().getTargetEntity();
            String key = event.getEntry().getKey();
            MergeCacheManager.this.addCacheEntry(documentReference, key);
        }

        @Override
        public void cacheEntryRemoved(CacheEntryEvent<ChangeRequestMergeDocumentResult> event)
        {
            DocumentReference documentReference = event.getEntry().getValue().getFileChange().getTargetEntity();
            String key = event.getEntry().getKey();
            MergeCacheManager.this.removeCacheEntry(documentReference, key);
        }

        @Override
        public void cacheEntryModified(CacheEntryEvent<ChangeRequestMergeDocumentResult> event)
        {
            // Nothing to do.
        }
    }

    private void addCacheEntry(DocumentReference documentReference, String key)
    {
        Set<String> listKeys;
        if (this.cacheKeysMap.containsKey(documentReference)) {
            listKeys = MergeCacheManager.this.cacheKeysMap.get(documentReference);
        } else {
            listKeys = new HashSet<>();
            MergeCacheManager.this.cacheKeysMap.put(documentReference, listKeys);
        }
        listKeys.add(key);
    }

    private void removeCacheEntry(DocumentReference documentReference, String key)
    {
        if (this.cacheKeysMap.containsKey(documentReference)) {
            Set<String> listKeys = this.cacheKeysMap.get(documentReference);
            listKeys.remove(key);
            if (listKeys.isEmpty()) {
                this.cacheKeysMap.remove(documentReference);
            }
        }
    }

    @Override
    public void initialize() throws InitializationException
    {
        try {
            this.hasConflictCache =
                this.cacheManager.createNewCache(new LRUCacheConfiguration("changerequest.hasConflictCache", 1000));
            this.hasConflictCache.addCacheEntryListener(new ConflictCacheEntryListener());
            this.crMergeDocumentResultCache =
                this.cacheManager.createNewCache(new LRUCacheConfiguration("changerequest.crMergeDocumentResult", 100));
            this.crMergeDocumentResultCache.addCacheEntryListener(new CRMergeDocumentResultCacheEntryListener());
        } catch (CacheException e) {
            throw new InitializationException("Error when initializing the cache for merge results.", e);
        }
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        this.hasConflictCache.dispose();
        this.crMergeDocumentResultCache.dispose();
    }

    private String getCacheKey(FileChange fileChange)
    {
        return String.format("%s-%s", fileChange.getId(), fileChange.getChangeRequest().getId());
    }

    /**
     * Look in the cache if there is a conflict value for the given filechange.
     *
     * @param fileChange the filechange for which to check if there's a conflict.
     * @return {@link Optional#empty()} if there's no value in cache, else an optional containing the boolean result.
     */
    public Optional<Boolean> hasConflict(FileChange fileChange)
    {
        Pair<DocumentReference, Boolean> pair =
            this.hasConflictCache.get(this.getCacheKey(fileChange));
        if (pair != null) {
            return Optional.of(pair.getRight());
        } else {
            return Optional.empty();
        }
    }

    /**
     * Put in cache the conflict value of the given filechange.
     *
     * @param fileChange the filechange for which to put in cache the conflict value.
     * @param status the conflict value of the given filechange.
     */
    public void setConflictStatus(FileChange fileChange, boolean status)
    {
        this.hasConflictCache.set(getCacheKey(fileChange), Pair.of(fileChange.getTargetEntity(), status));
    }

    /**
     * Search in cache if there's already a {@link ChangeRequestMergeDocumentResult} for the given filechange.
     *
     * @param fileChange the filechange for which to retrieve a merge document result.
     * @return {@link Optional#empty()} if there's no record in cache, or an optional containing the actual merge
     *          document result.
     */
    public Optional<ChangeRequestMergeDocumentResult> getChangeRequestMergeDocumentResult(FileChange fileChange)
    {
        ChangeRequestMergeDocumentResult changeRequestMergeDocumentResult =
            this.crMergeDocumentResultCache.get(getCacheKey(fileChange));
        if (changeRequestMergeDocumentResult != null) {
            return Optional.of(changeRequestMergeDocumentResult);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Put in cache the {@link ChangeRequestMergeDocumentResult} of  the given filechange.
     *
     * @param fileChange the filechange for which to put in cache the merge document result.
     * @param changeRequestMergeDocumentResult the merge document result to keep in cache.
     */
    public void setChangeRequestMergeDocumentResult(FileChange fileChange,
        ChangeRequestMergeDocumentResult changeRequestMergeDocumentResult)
    {
        this.crMergeDocumentResultCache.set(getCacheKey(fileChange), changeRequestMergeDocumentResult);
    }

    /**
     * Invalidate all entries matching the given reference: note that the invalidation will impact all filechanges from
     * any changerequest concerning the given document reference.
     *
     * @param documentReference the reference for which to invalidate the cache entries.
     */
    public void invalidate(DocumentReference documentReference)
    {
        if (this.cacheKeysMap.containsKey(documentReference)) {
            for (String key : this.cacheKeysMap.get(documentReference)) {
                this.hasConflictCache.remove(key);
                this.crMergeDocumentResultCache.remove(key);
            }
        }
    }

    /**
     * Invalidate all entries corresponding to the given filechange.
     *
     * @param fileChange the filechange for which to invalidate the cache entries.
     */
    public void invalidate(FileChange fileChange)
    {
        this.hasConflictCache.remove(getCacheKey(fileChange));
        this.crMergeDocumentResultCache.remove(getCacheKey(fileChange));
    }
}
