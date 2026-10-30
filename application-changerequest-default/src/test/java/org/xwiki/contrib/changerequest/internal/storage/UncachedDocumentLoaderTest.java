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
package org.xwiki.contrib.changerequest.internal.storage;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.XWikiCacheStoreInterface;
import com.xpn.xwiki.store.XWikiStoreInterface;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link UncachedDocumentLoader}.
 *
 * @version $Id$
 */
@ComponentTest
class UncachedDocumentLoaderTest
{
    private static final DocumentReference REFERENCE = new DocumentReference("subwiki", "Space", "Page");

    @InjectMockComponents
    private UncachedDocumentLoader loader;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    private XWikiContext context;

    private XWiki wiki;

    @BeforeEach
    void beforeEach()
    {
        this.context = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(this.context);
        this.wiki = mock(XWiki.class);
        when(this.context.getWiki()).thenReturn(this.wiki);
        when(this.context.getWikiId()).thenReturn("currentwiki");
    }

    @Test
    void loadForUpdateBypassesTheDocumentCache() throws Exception
    {
        XWikiCacheStoreInterface cacheStore = mock(XWikiCacheStoreInterface.class);
        XWikiStoreInterface persistentStore = mock(XWikiStoreInterface.class);
        when(this.wiki.getStore()).thenReturn(cacheStore);
        when(cacheStore.getStore()).thenReturn(persistentStore);

        XWikiDocument storedDocument = mock(XWikiDocument.class);
        when(persistentStore.loadXWikiDoc(argThat(doc -> REFERENCE.equals(doc.getDocumentReference())),
            eq(this.context))).thenReturn(storedDocument);

        assertSame(storedDocument, this.loader.loadForUpdate(REFERENCE));

        verify(cacheStore, never()).loadXWikiDoc(any(), any());
        InOrder inOrder = inOrder(this.context, persistentStore);
        inOrder.verify(this.context).setWikiId("subwiki");
        inOrder.verify(persistentStore).loadXWikiDoc(any(), eq(this.context));
        inOrder.verify(this.context).setWikiId("currentwiki");
    }

    @Test
    void loadForUpdateWithoutDocumentCache() throws Exception
    {
        XWikiStoreInterface persistentStore = mock(XWikiStoreInterface.class);
        when(this.wiki.getStore()).thenReturn(persistentStore);

        XWikiDocument storedDocument = mock(XWikiDocument.class);
        when(persistentStore.loadXWikiDoc(argThat(doc -> REFERENCE.equals(doc.getDocumentReference())),
            eq(this.context))).thenReturn(storedDocument);

        assertSame(storedDocument, this.loader.loadForUpdate(REFERENCE));
        verify(this.context).setWikiId("currentwiki");
    }
}
