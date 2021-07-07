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
package org.xwiki.contrib.changerequest.internal.handlers;

import java.util.Optional;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestManager;
import org.xwiki.contrib.changerequest.ChangeRequestReference;
import org.xwiki.contrib.changerequest.internal.ChangeRequestDocumentReferenceResolver;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiResponse;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link MergeChangeRequestHandler}.
 *
 * @version $Id$
 * @since 0.3
 */
@ComponentTest
class MergeChangeRequestHandlerTest
{
    @InjectMockComponents
    private MergeChangeRequestHandler handler;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private ChangeRequestManager changeRequestManager;

    @MockComponent
    private ChangeRequestStorageManager storageManager;

    @MockComponent
    private UserReferenceResolver<CurrentUserReference> userReferenceResolver;

    @MockComponent
    private DocumentReferenceResolver<ChangeRequest> resolver;

    private XWikiContext context;
    private XWikiResponse response;

    @BeforeEach
    void setup()
    {
        this.context = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(this.context);

        this.response = mock(XWikiResponse.class);
        when(this.context.getResponse()).thenReturn(this.response);
    }

    @Test
    void handle() throws Exception
    {
        ChangeRequestReference changeRequestReference = mock(ChangeRequestReference.class);
        String id = "cr43";
        when(changeRequestReference.getId()).thenReturn(id);
        when(this.storageManager.load(id)).thenReturn(Optional.empty());

        this.handler.handle(changeRequestReference);
        verify(this.response).sendError(404, "Cannot find change request with id [cr43]");

        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(this.storageManager.load(id)).thenReturn(Optional.of(changeRequest));
        UserReference userReference = mock(UserReference.class);
        when(this.userReferenceResolver.resolve(CurrentUserReference.INSTANCE)).thenReturn(userReference);
        when(this.changeRequestManager.isAuthorizedToMerge(userReference, changeRequest)).thenReturn(false);

        this.handler.handle(changeRequestReference);
        verify(this.response).sendError(403, "You're not authorized to merge change request [cr43].");

        when(this.changeRequestManager.isAuthorizedToMerge(userReference, changeRequest)).thenReturn(true);
        when(this.changeRequestManager.canBeMerged(changeRequest)).thenReturn(false);

        this.handler.handle(changeRequestReference);
        verify(this.response).sendError(409, "The change request [cr43] cannot be merged.");

        when(this.changeRequestManager.canBeMerged(changeRequest)).thenReturn(true);
        DocumentReference documentReference = mock(DocumentReference.class);
        when(this.resolver.resolve(changeRequest)).thenReturn(documentReference);
        XWiki wiki = mock(XWiki.class);
        when(this.context.getWiki()).thenReturn(wiki);
        when(wiki.getURL(documentReference, this.context)).thenReturn("/my/change/request");

        this.handler.handle(changeRequestReference);
        verify(this.storageManager).merge(changeRequest);
        verify(this.response).sendRedirect("/my/change/request");
    }
}
