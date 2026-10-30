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
package org.xwiki.contrib.changerequest.internal.approvers;

import java.util.Set;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.contrib.changerequest.ApproversManager;
import org.xwiki.contrib.changerequest.internal.storage.UncachedDocumentLoader;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link DocumentReferenceApproversManager}.
 *
 * @version $Id$
 */
@ComponentTest
class DocumentReferenceApproversManagerTest
{
    private static final DocumentReference REFERENCE = new DocumentReference("xwiki", "Space", "Page");

    @InjectMockComponents
    private DocumentReferenceApproversManager manager;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private ApproversManager<XWikiDocument> documentApproversManager;

    @MockComponent
    private UncachedDocumentLoader uncachedDocumentLoader;

    private XWikiContext context;

    private XWiki wiki;

    @BeforeEach
    void beforeEach()
    {
        this.context = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(this.context);
        this.wiki = mock(XWiki.class);
        when(this.context.getWiki()).thenReturn(this.wiki);
    }

    @Test
    void setApproversModifiesTheStoredDocument() throws Exception
    {
        XWikiDocument storedDocument = mock(XWikiDocument.class);
        when(this.uncachedDocumentLoader.loadForUpdate(REFERENCE)).thenReturn(storedDocument);
        Set<UserReference> users = Set.of(mock(UserReference.class));
        Set<DocumentReference> groups = Set.of(new DocumentReference("xwiki", "XWiki", "Group"));

        this.manager.setUsersApprovers(users, REFERENCE);
        this.manager.setGroupsApprovers(groups, REFERENCE);

        verify(this.documentApproversManager).setUsersApprovers(users, storedDocument);
        verify(this.documentApproversManager).setGroupsApprovers(groups, storedDocument);
    }

    @Test
    void isApproverReadsTheCachedDocument() throws Exception
    {
        XWikiDocument cachedDocument = mock(XWikiDocument.class);
        when(this.wiki.getDocument(REFERENCE, this.context)).thenReturn(cachedDocument);
        UserReference user = mock(UserReference.class);
        when(this.documentApproversManager.isApprover(user, cachedDocument, true)).thenReturn(true);

        assertTrue(this.manager.isApprover(user, REFERENCE, true));
    }
}
