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

import org.junit.jupiter.api.Test;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ChangeRequestStorageCacheManager}.
 *
 * @version $Id$
 * @since 1.24
 */
@ComponentTest
class ChangeRequestStorageCacheManagerTest
{
    private static final String CR_ID = "crId";

    @InjectMockComponents
    private ChangeRequestStorageCacheManager cacheManager;

    @MockComponent
    private CacheManager wikiCacheManager;

    private Map<String, Object> cacheContent;

    @BeforeComponent
    void beforeComponent() throws CacheException
    {
        this.cacheContent = new HashMap<>();
        Cache<Object> cache = mock(Cache.class);
        when(cache.get(anyString())).thenAnswer(invocation -> this.cacheContent.get(invocation.getArgument(0)));
        doAnswer(invocation -> this.cacheContent.put(invocation.getArgument(0), invocation.getArgument(1)))
            .when(cache).set(anyString(), any());
        doAnswer(invocation -> this.cacheContent.remove(invocation.getArgument(0))).when(cache).remove(anyString());
        doAnswer(invocation -> {
            this.cacheContent.clear();
            return null;
        }).when(cache).removeAll();
        when(this.wikiCacheManager.createNewCache(any())).thenReturn((Cache) cache);
    }

    @Test
    void cacheAndRetrieveChangeRequest()
    {
        ChangeRequest changeRequest = new ChangeRequest().setId(CR_ID);
        assertTrue(this.cacheManager.getChangeRequest(CR_ID).isEmpty());

        long generation = this.cacheManager.startLoading(CR_ID);
        // Starting a load must not make the change request available yet.
        assertTrue(this.cacheManager.getChangeRequest(CR_ID).isEmpty());

        this.cacheManager.cacheChangeRequest(changeRequest, generation);
        assertSame(changeRequest, this.cacheManager.getChangeRequest(CR_ID).get());
    }

    @Test
    void invalidateDropsTheCachedChangeRequest()
    {
        ChangeRequest changeRequest = new ChangeRequest().setId(CR_ID);
        this.cacheManager.cacheChangeRequest(changeRequest, this.cacheManager.startLoading(CR_ID));
        assertSame(changeRequest, this.cacheManager.getChangeRequest(CR_ID).get());

        this.cacheManager.invalidate(CR_ID);
        assertTrue(this.cacheManager.getChangeRequest(CR_ID).isEmpty());
    }

    @Test
    void cacheChangeRequestInvalidatedWhileBeingLoaded()
    {
        ChangeRequest changeRequest = new ChangeRequest().setId(CR_ID);
        long generation = this.cacheManager.startLoading(CR_ID);

        // Something saved the change request while it was being loaded: what has been read is already outdated.
        this.cacheManager.invalidate(CR_ID);

        this.cacheManager.cacheChangeRequest(changeRequest, generation);
        assertTrue(this.cacheManager.getChangeRequest(CR_ID).isEmpty());
    }

    @Test
    void cacheChangeRequestWhoseEntryDisappearedWhileBeingLoaded()
    {
        ChangeRequest changeRequest = new ChangeRequest().setId(CR_ID);
        long generation = this.cacheManager.startLoading(CR_ID);

        // The entry holding the generation is gone, from invalidateAll here, but an eviction has the same effect:
        // whether an invalidation happened cannot be told anymore, so the change request must not be cached.
        this.cacheManager.invalidateAll();

        this.cacheManager.cacheChangeRequest(changeRequest, generation);
        assertTrue(this.cacheManager.getChangeRequest(CR_ID).isEmpty());
    }

    @Test
    void cacheChangeRequestLoadedAfterAnInvalidation()
    {
        ChangeRequest changeRequest = new ChangeRequest().setId(CR_ID);
        this.cacheManager.invalidate(CR_ID);

        long generation = this.cacheManager.startLoading(CR_ID);
        this.cacheManager.cacheChangeRequest(changeRequest, generation);
        assertSame(changeRequest, this.cacheManager.getChangeRequest(CR_ID).get());
    }

    @Test
    void cacheChangeRequestOfALoaderOutrunByAFresherOne()
    {
        // Two change requests with the same identifier are equal, so identity is what the assertions rely on here.
        ChangeRequest outdated = new ChangeRequest().setId(CR_ID);
        ChangeRequest upToDate = new ChangeRequest().setId(CR_ID);

        // A reader starts before the change request is saved.
        long outdatedGeneration = this.cacheManager.startLoading(CR_ID);

        // The save invalidates the cache, and a later request loads the up to date data and caches it.
        this.cacheManager.invalidate(CR_ID);
        this.cacheManager.cacheChangeRequest(upToDate, this.cacheManager.startLoading(CR_ID));

        // The first reader only finishes now: its result must not replace the up to date one.
        this.cacheManager.cacheChangeRequest(outdated, outdatedGeneration);
        assertSame(upToDate, this.cacheManager.getChangeRequest(CR_ID).get());
    }
}
