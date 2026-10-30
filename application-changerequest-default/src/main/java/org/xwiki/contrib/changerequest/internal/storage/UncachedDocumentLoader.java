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

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.XWikiCacheStoreInterface;
import com.xpn.xwiki.store.XWikiStoreInterface;

/**
 * Load a document from the persistent storage, bypassing the document cache, so that it can be modified and saved.
 * <p>
 * The document cache of XWiki can hold an older version of a document than the one stored: a load started before a
 * save can put the version it read in the cache after the save removed it from there (XWIKI-23055: The document cache
 * may not be properly invalidated in case of concurrent loads, fixed in XWiki 16.10.9). Saving a document modified
 * from such a version reverts every change saved since, so the documents that are saved several times in a row, such
 * as the change request documents, must be modified from the stored version.
 * <p>
 * This component is only needed while the parent version is older than XWiki 16.10.9 or 17.3.0: from those versions,
 * the document cache drops a loaded document when it has been invalidated during the load. It should then be removed,
 * and its callers should modify a clone of the cached document instead, i.e. {@code
 * context.getWiki().getDocument(reference, context).clone()}: the clone is required since the cached instance is
 * shared with the other threads.
 *
 * @version $Id$
 * @since 1.26
 */
@Component(roles = UncachedDocumentLoader.class)
@Singleton
public class UncachedDocumentLoader
{
    @Inject
    private Provider<XWikiContext> contextProvider;

    /**
     * Load the document from the persistent storage.
     *
     * @param reference the reference of the document to load
     * @return a new instance of the document as it is stored, that can be modified without cloning it, or a new
     *         document if it doesn't exist
     * @throws XWikiException in case of problem when loading the document
     */
    public XWikiDocument loadForUpdate(DocumentReference reference) throws XWikiException
    {
        XWikiContext context = this.contextProvider.get();
        XWikiStoreInterface store = context.getWiki().getStore();
        if (store instanceof XWikiCacheStoreInterface) {
            store = ((XWikiCacheStoreInterface) store).getStore();
        }
        String currentWiki = context.getWikiId();
        try {
            context.setWikiId(reference.getWikiReference().getName());
            return store.loadXWikiDoc(new XWikiDocument(reference), context);
        } finally {
            context.setWikiId(currentWiki);
        }
    }
}
