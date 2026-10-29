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

import javax.inject.Named;
import javax.script.ScriptContext;
import javax.servlet.http.HttpSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.csrf.CSRFToken;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.store.TemporaryAttachmentSessionsManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.user.UserReferenceSerializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;
import com.xpn.xwiki.web.XWikiRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link EditChangeRequestResourceHandler}.
 *
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
class EditChangeRequestResourceHandlerTest
{
    private static final DocumentReference USER_DOCUMENT_REFERENCE = new DocumentReference("wiki", "XWiki", "user");

    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("wiki", "space", "page");

    private static final UserReference USER_REFERENCE = mock(UserReference.class);

    private static final UserReference OTHERUSER_REFERENCE = mock(UserReference.class);

    @InjectMockComponents
    private EditChangeRequestResourceHandler handler;

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @MockComponent
    private ChangeRequestStorageManager changeRequestStorageManager;

    @MockComponent
    private TemporaryAttachmentSessionsManager temporaryAttachmentSessionsManager;

    @MockComponent
    private ContextualLocalizationManager contextualLocalizationManager;

    @MockComponent
    private ScriptContextManager scriptContextManager;

    @MockComponent
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @MockComponent
    @Named("document")
    private UserReferenceResolver<DocumentReference> documentReferenceUserReferenceResolver;

    @MockComponent
    @Named("document")
    private UserReferenceSerializer<DocumentReference> documentReferenceUserReferenceSerializer;

    @MockComponent
    private CSRFToken csrf;

    private XWikiContext context;

    private XWikiRequest request;

    @BeforeEach
    void beforeEach()
    {
        when(this.documentReferenceUserReferenceResolver.resolve(USER_DOCUMENT_REFERENCE)).thenReturn(USER_REFERENCE);
        when(this.documentReferenceUserReferenceSerializer.serialize(USER_REFERENCE))
            .thenReturn(USER_DOCUMENT_REFERENCE);
        when(this.scriptContextManager.getCurrentScriptContext()).thenReturn(mock(ScriptContext.class));

        this.context = this.oldcore.getXWikiContext();
        this.context.setUserReference(USER_DOCUMENT_REFERENCE);

        this.request = mock(XWikiRequest.class);
        when(this.request.getSession()).thenReturn(mock(HttpSession.class));
        when(this.request.getParameter("form_token")).thenReturn("tokenvalue");
        this.context.setRequest(this.request);
    }

    private XWikiDocument saveDocumentAuthoredByOtherUser() throws Exception
    {
        XWikiDocument document = this.oldcore.getSpyXWiki().getDocument(DOCUMENT_REFERENCE, this.context);
        document.setContent("saved content");
        document.getAuthors().setCreator(OTHERUSER_REFERENCE);
        document.getAuthors().setContentAuthor(OTHERUSER_REFERENCE);
        document.getAuthors().setEffectiveMetadataAuthor(OTHERUSER_REFERENCE);
        document.getAuthors().setOriginalMetadataAuthor(OTHERUSER_REFERENCE);
        this.oldcore.getSpyXWiki().saveDocument(document, this.context);

        document = this.oldcore.getSpyXWiki().getDocument(DOCUMENT_REFERENCE, this.context);
        this.context.setDoc(document);
        this.context.put("tdoc", document);
        return document;
    }

    @Test
    void documentAuthorsWhenDocumentExistAndIsNotModified() throws Exception
    {
        saveDocumentAuthoredByOtherUser();

        XWikiDocument editedDocument = this.handler.prepareEditedDocument(this.context, null);

        assertEquals("saved content", editedDocument.getContent());
        assertSame(OTHERUSER_REFERENCE, editedDocument.getAuthors().getContentAuthor());
        assertSame(OTHERUSER_REFERENCE, editedDocument.getAuthors().getCreator());
        assertSame(OTHERUSER_REFERENCE, editedDocument.getAuthors().getEffectiveMetadataAuthor());
        assertSame(OTHERUSER_REFERENCE, editedDocument.getAuthors().getOriginalMetadataAuthor());
        assertFalse(editedDocument.isRestricted());
        verifyNoInteractions(this.csrf);
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void documentAuthorsWhenDocumentExistAndContentIsModified(boolean validToken) throws Exception
    {
        saveDocumentAuthoredByOtherUser();
        when(this.request.getParameter("content")).thenReturn("modified content");
        when(this.csrf.isTokenValid("tokenvalue")).thenReturn(validToken);

        XWikiDocument editedDocument = this.handler.prepareEditedDocument(this.context, null);

        assertEquals("modified content", editedDocument.getContent());
        assertSame(USER_REFERENCE, editedDocument.getAuthors().getContentAuthor());
        assertSame(OTHERUSER_REFERENCE, editedDocument.getAuthors().getCreator());
        assertEquals(!validToken, editedDocument.isRestricted());
        assertSame(editedDocument, this.context.get("tdoc"));
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void documentAuthorsWhenChangeRequestDocumentContentIsModified(boolean validToken) throws Exception
    {
        XWikiDocument modifiedDocument = saveDocumentAuthoredByOtherUser().clone();
        // The document stored in a change request is restricted.
        modifiedDocument.setRestricted(true);
        when(this.request.getParameter("content")).thenReturn("modified content");
        when(this.csrf.isTokenValid("tokenvalue")).thenReturn(validToken);

        XWikiDocument editedDocument = this.handler.prepareEditedDocument(this.context, modifiedDocument);

        assertEquals("modified content", editedDocument.getContent());
        assertSame(USER_REFERENCE, editedDocument.getAuthors().getContentAuthor());
        assertSame(OTHERUSER_REFERENCE, editedDocument.getAuthors().getCreator());
        assertEquals(!validToken, editedDocument.isRestricted());
    }
}
