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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.contrib.changerequest.storage.FileChangeStorageManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ChangeRequestTitleCacheManager}
 *
 * @version $Id$
 * @since 1.4.5
 */
@ComponentTest
public class ChangeRequestTitleCacheManagerTest
{
    @InjectMockComponents
    private ChangeRequestTitleCacheManager changeRequestTitleCacheManager;

    @MockComponent
    private ChangeRequestStorageManager changeRequestStorageManager;

    @MockComponent
    private FileChangeStorageManager fileChangeStorageManager;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private CacheManager cacheManager;

    private Cache<Map<String, String>> titleCache;
    private XWikiContext context;

    @BeforeComponent
    void beforeComponent() throws CacheException
    {
        this.titleCache = mock(Cache.class);
        when(this.cacheManager.createNewCache(any())).thenReturn((Cache)this.titleCache);
    }

    @BeforeEach
    void setup()
    {
        this.context = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(this.context);
    }

    @Test
    void getTitle() throws ChangeRequestException
    {
        String crId = "crId";
        String fileChangeId = "fileChangeId";
        String olderFileChangeId = "olderId";
        String expectedTitle = "Some title";
        String olderTitle = "An older title";

        when(this.titleCache.get(crId)).thenReturn(Collections.singletonMap(fileChangeId, expectedTitle));
        assertEquals(expectedTitle, this.changeRequestTitleCacheManager.getTitle(crId, fileChangeId));
        verifyNoInteractions(this.fileChangeStorageManager);
        verifyNoInteractions(this.changeRequestStorageManager);

        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(changeRequest.getId()).thenReturn(crId);
        FileChange latestFileChange = mock(FileChange.class, "latest");
        when(latestFileChange.getId()).thenReturn(fileChangeId);
        FileChange olderFileChange = mock(FileChange.class, "older");
        when(olderFileChange.getId()).thenReturn(olderFileChangeId);
        when(latestFileChange.getChangeRequest()).thenReturn(changeRequest);
        when(olderFileChange.getChangeRequest()).thenReturn(changeRequest);

        DocumentReference ref = mock(DocumentReference.class);
        when(latestFileChange.getTargetEntity()).thenReturn(ref);
        when(olderFileChange.getTargetEntity()).thenReturn(ref);

        when(changeRequest.getLatestFileChangeFor(ref)).thenReturn(Optional.of(latestFileChange));
        when(changeRequest.getAllFileChanges()).thenReturn(List.of(olderFileChange, latestFileChange));
        when(changeRequest.getFileChangeImmediatelyBefore(latestFileChange)).thenReturn(Optional.of(olderFileChange));

        when(this.changeRequestStorageManager.load(crId)).thenReturn(Optional.of(changeRequest));
        XWikiDocument document = mock(XWikiDocument.class);
        when(latestFileChange.getModifiedDocument()).thenReturn(document);
        when(document.getRenderedTitle(this.context)).thenReturn(expectedTitle);

        Map<String, String> myMap = new HashMap<>();
        myMap.put(olderFileChangeId, olderTitle);
        when(this.titleCache.get(crId)).thenReturn(myMap);
        assertEquals(expectedTitle, this.changeRequestTitleCacheManager.getTitle(crId, fileChangeId));

        Map<String, String> expectedMap = new HashMap<>();
        expectedMap.put(olderFileChangeId, expectedTitle);
        expectedMap.put(fileChangeId, expectedTitle);
        assertEquals(expectedMap, myMap);
        verifyNoInteractions(this.fileChangeStorageManager);

        when(this.titleCache.get(crId)).thenReturn(null);
        expectedMap = new HashMap<>();
        expectedMap.put(olderFileChangeId, expectedTitle);

        when(latestFileChange.getModifiedDocument()).thenReturn(null);
        when(this.fileChangeStorageManager.getCurrentDocumentFromFileChange(latestFileChange)).thenReturn(document);
        assertEquals(expectedTitle, this.changeRequestTitleCacheManager.getTitle(crId, olderFileChangeId));
        verify(olderFileChange, never()).getModifiedDocument();
        verify(titleCache).set(crId, expectedMap);
    }
}
