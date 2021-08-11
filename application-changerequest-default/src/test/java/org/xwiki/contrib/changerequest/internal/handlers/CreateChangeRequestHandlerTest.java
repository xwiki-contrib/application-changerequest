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

import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.inject.Provider;
import org.junit.jupiter.api.Test;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestStatus;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.events.ChangeRequestCreatedEvent;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.observation.ObservationManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.wysiwyg.converter.RequestParameterConverter;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link CreateChangeRequestHandler}.
 *
 * @version $Id$
 * @since 0.3
 */
@ComponentTest
class CreateChangeRequestHandlerTest
{
    @InjectMockComponents
    private CreateChangeRequestHandler handler;

    @MockComponent
    protected Provider<XWikiContext> contextProvider;

    @MockComponent
    protected DocumentReferenceResolver<ChangeRequest> changeRequestDocumentReferenceResolver;

    @MockComponent
    protected ChangeRequestStorageManager storageManager;

    @MockComponent
    protected ObservationManager observationManager;

    @MockComponent
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @MockComponent
    private RequestParameterConverter requestParameterConverter;

    @MockComponent
    private UserReferenceResolver<CurrentUserReference> userReferenceResolver;

    private XWikiContext context;
    private XWikiRequest httpServletRequest;
    private XWikiResponse httpServletResponse;
    private XWiki xWiki;

    @Test
    void handle() throws Exception
    {
        this.context = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(this.context);
        this.httpServletRequest = mock(XWikiRequest.class);
        when(this.context.getRequest()).thenReturn(this.httpServletRequest);
        this.httpServletResponse = mock(XWikiResponse.class);
        when(this.context.getResponse()).thenReturn(this.httpServletResponse);
        this.xWiki = mock(XWiki.class);
        when(this.context.getWiki()).thenReturn(this.xWiki);

        when(this.requestParameterConverter.convert(this.httpServletRequest, this.httpServletResponse))
            .thenReturn(Optional.of(this.httpServletRequest));
        String serializedReference = "XWiki.SomeReference";
        when(this.httpServletRequest.getParameter("docReference")).thenReturn(serializedReference);
        DocumentReference documentReference = mock(DocumentReference.class);
        when(this.documentReferenceResolver.resolve(serializedReference)).thenReturn(documentReference);
        XWikiDocument modifiedDocument = mock(XWikiDocument.class);
        when(this.xWiki.getDocument(documentReference, this.context)).thenReturn(modifiedDocument);
        when(modifiedDocument.clone()).thenReturn(modifiedDocument);
        DocumentReference documentReferenceWithLocale = mock(DocumentReference.class);
        when(modifiedDocument.getDocumentReferenceWithLocale()).thenReturn(documentReferenceWithLocale);

        String title = "some title";
        String description = "some description";
        when(this.httpServletRequest.getParameter("crTitle")).thenReturn(title);
        when(this.httpServletRequest.getParameter("crDescription")).thenReturn(description);

        UserReference userReference = mock(UserReference.class);
        when(this.userReferenceResolver.resolve(CurrentUserReference.INSTANCE)).thenReturn(userReference);
        String previousVersion = "3.2";
        when(this.httpServletRequest.getParameter("previousVersion")).thenReturn(previousVersion);
        ChangeRequest expectedChangeRequest = new ChangeRequest();
        FileChange expectedFileChange = new FileChange(expectedChangeRequest);
        expectedFileChange
            .setAuthor(userReference)
            .setTargetEntity(documentReferenceWithLocale)
            .setPreviousVersion(previousVersion)
            .setModifiedDocument(modifiedDocument);

        String crId = "myCrID";
        expectedChangeRequest
            .setId(crId)
            .setTitle(title)
            .setDescription(description)
            .setCreator(userReference)
            .addFileChange(expectedFileChange)
            .setStatus(ChangeRequestStatus.READY_FOR_REVIEW);

        doAnswer(invocationOnMock -> {
            ChangeRequest changeRequest = invocationOnMock.getArgument(0);
            List<FileChange> allFileChanges = changeRequest.getAllFileChanges();
            assertEquals(1, allFileChanges.size());
            // ensure to have the exact same date in file change
            Date creationDate = allFileChanges.get(0).getCreationDate();
            expectedFileChange.setCreationDate(creationDate);
            expectedChangeRequest.setCreationDate(changeRequest.getCreationDate());
            changeRequest.setId(crId);
            return null;
        }).when(this.storageManager).save(any());

        DocumentReference crDocReference = mock(DocumentReference.class);
        when(this.changeRequestDocumentReferenceResolver.resolve(expectedChangeRequest)).thenReturn(crDocReference);
        String expectedURL = "/mycr";
        when(this.xWiki.getURL(crDocReference, this.context)).thenReturn(expectedURL);

        this.handler.handle(null);
        verify(this.storageManager).save(expectedChangeRequest);
        verify(this.observationManager)
            .notify(any(ChangeRequestCreatedEvent.class), eq(documentReferenceWithLocale), eq(crId));
        verify(this.httpServletResponse).sendRedirect(expectedURL);
    }

}
