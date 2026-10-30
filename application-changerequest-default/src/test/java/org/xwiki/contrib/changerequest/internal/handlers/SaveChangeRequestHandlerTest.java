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

import java.io.PrintWriter;
import java.util.Optional;

import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.container.servlet.ServletResponse;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestReference;
import org.xwiki.contrib.changerequest.ChangeRequestRightsManager;
import org.xwiki.contrib.changerequest.events.ChangeRequestUpdatedEvent;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.csrf.CSRFToken;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.observation.ObservationManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link SaveChangeRequestHandler}.
 *
 * @version $Id$
 */
@ComponentTest
class SaveChangeRequestHandlerTest
{
    private static final String CR_ID = "crId";

    @InjectMockComponents
    private SaveChangeRequestHandler handler;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private Container container;

    @MockComponent
    private CSRFToken csrfToken;

    @MockComponent
    private ChangeRequestStorageManager storageManager;

    @MockComponent
    private ChangeRequestRightsManager changeRequestRightsManager;

    @MockComponent
    private UserReferenceResolver<CurrentUserReference> currentUserReferenceResolver;

    @MockComponent
    private ContextualLocalizationManager contextualLocalizationManager;

    @MockComponent
    private DocumentReferenceResolver<ChangeRequest> changeRequestDocumentReferenceResolver;

    @MockComponent
    private ObservationManager observationManager;

    private XWikiContext context;

    private XWiki xwiki;

    private HttpServletRequest httpServletRequest;

    private XWikiResponse xwikiResponse;

    private ChangeRequestReference changeRequestReference;

    private ChangeRequest changeRequest;

    @BeforeEach
    void setup() throws Exception
    {
        this.context = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(this.context);
        this.xwiki = mock(XWiki.class);
        when(this.context.getWiki()).thenReturn(this.xwiki);
        when(this.context.getRequest()).thenReturn(mock(XWikiRequest.class));
        this.xwikiResponse = mock(XWikiResponse.class);
        when(this.context.getResponse()).thenReturn(this.xwikiResponse);

        this.httpServletRequest = mock(HttpServletRequest.class);
        ServletRequest servletRequest = mock(ServletRequest.class);
        when(servletRequest.getHttpServletRequest()).thenReturn(this.httpServletRequest);
        when(this.container.getRequest()).thenReturn(servletRequest);
        ServletResponse servletResponse = mock(ServletResponse.class);
        when(servletResponse.getHttpServletResponse()).thenReturn(mock(HttpServletResponse.class));
        when(this.container.getResponse()).thenReturn(servletResponse);

        when(this.httpServletRequest.getMethod()).thenReturn("POST");
        when(this.httpServletRequest.getParameter("form_token")).thenReturn("token");
        when(this.csrfToken.isTokenValid("token")).thenReturn(true);
        when(this.httpServletRequest.getParameter("changeType")).thenReturn("settitle");

        this.changeRequestReference = mock(ChangeRequestReference.class);
        when(this.changeRequestReference.getId()).thenReturn(CR_ID);
        this.changeRequest = new ChangeRequest().setId(CR_ID).setTitle("Old title");
        when(this.storageManager.load(CR_ID)).thenReturn(Optional.of(this.changeRequest));

        UserReference currentUser = mock(UserReference.class);
        when(this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE)).thenReturn(currentUser);
        when(this.changeRequestRightsManager.isAuthorizedToEdit(currentUser, this.changeRequest)).thenReturn(true);
    }

    @Test
    void handleTitleUpdate() throws Exception
    {
        when(this.httpServletRequest.getParameter("new-title")).thenReturn("New title #123");
        when(this.contextualLocalizationManager.getTranslationPlain("changerequest.save.title"))
            .thenReturn("Title changed");
        DocumentReference changeRequestDocReference = mock(DocumentReference.class);
        when(this.changeRequestDocumentReferenceResolver.resolve(this.changeRequest))
            .thenReturn(changeRequestDocReference);
        when(this.xwiki.getURL(changeRequestDocReference, "view", this.context)).thenReturn("/cr");

        this.handler.handle(this.changeRequestReference);

        assertEquals("New title #123", this.changeRequest.getTitle());
        verify(this.storageManager).save(this.changeRequest, "Title changed");
        verify(this.observationManager).notify(any(ChangeRequestUpdatedEvent.class), eq(CR_ID),
            eq(this.changeRequest));
        verify(this.xwikiResponse).sendRedirect("/cr");
    }

    @Test
    void handleTitleUpdateWithScript() throws Exception
    {
        when(this.httpServletRequest.getParameter("new-title")).thenReturn("New title $name");
        when(this.contextualLocalizationManager.getTranslationPlain("changerequest.save.error.titleScript"))
            .thenReturn("Title error");
        when(this.xwiki.getEncoding()).thenReturn("UTF-8");
        PrintWriter printWriter = mock(PrintWriter.class);
        when(this.xwikiResponse.getWriter()).thenReturn(printWriter);

        this.handler.handle(this.changeRequestReference);

        assertEquals("Old title", this.changeRequest.getTitle());
        verify(this.xwikiResponse).setStatus(400);
        verify(printWriter).print("{\"changeRequestError\":\"Title error\"}");
        verify(this.storageManager, never()).save(any(), anyString());
        verify(this.observationManager, never()).notify(any(), any(), any());
        verify(this.xwikiResponse, never()).sendRedirect(anyString());
    }
}
