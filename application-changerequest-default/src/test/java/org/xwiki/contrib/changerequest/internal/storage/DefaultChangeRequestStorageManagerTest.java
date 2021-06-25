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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestStatus;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.internal.UserReferenceConverter;
import org.xwiki.contrib.changerequest.internal.id.ChangeRequestIDGenerator;
import org.xwiki.contrib.changerequest.storage.FileChangeStorageManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultChangeRequestStorageManager}.
 *
 * @version $Id$
 * @since 0.1
 */
@ComponentTest
class DefaultChangeRequestStorageManagerTest
{
    @InjectMockComponents
    private DefaultChangeRequestStorageManager storageManager;

    @MockComponent
    private UserReferenceConverter userReferenceConverter;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private FileChangeStorageManager fileChangeStorageManager;

    @MockComponent
    private DocumentReferenceResolver<ChangeRequest> changeRequestDocumentReferenceResolver;

    @MockComponent
    @Named("document")
    private UserReferenceResolver<DocumentReference> userReferenceResolver;

    @MockComponent
    @Named("title")
    private ChangeRequestIDGenerator idGenerator;

    private XWikiContext context;
    private XWiki wiki;

    @BeforeEach
    void setup()
    {
        this.context = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(this.context);
        this.wiki = mock(XWiki.class);
        when(this.context.getWiki()).thenReturn(this.wiki);
    }

    @Test
    void save() throws Exception
    {
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        String title = "a title";
        String description = "a description";
        when(changeRequest.getTitle()).thenReturn(title);
        when(changeRequest.getDescription()).thenReturn(description);
        when(changeRequest.getStatus()).thenReturn(ChangeRequestStatus.DRAFT);

        FileChange fileChange1 = mock(FileChange.class);
        FileChange fileChange2 = mock(FileChange.class);
        when(changeRequest.getFileChanges()).thenReturn(Arrays.asList(fileChange1, fileChange2));

        UserReference userReference = mock(UserReference.class);
        when(changeRequest.getCreator()).thenReturn(userReference);

        when(this.idGenerator.generateId(changeRequest)).thenReturn("id42");
        DocumentReference documentReference = mock(DocumentReference.class);
        when(this.changeRequestDocumentReferenceResolver.resolve(changeRequest)).thenReturn(documentReference);
        XWikiDocument document = mock(XWikiDocument.class);
        when(this.wiki.getDocument(documentReference, this.context)).thenReturn(document);

        DocumentReference userDocReference = mock(DocumentReference.class);
        when(this.userReferenceConverter.convert(userReference)).thenReturn(userDocReference);

        BaseObject xobject = mock(BaseObject.class);
        when(document.getXObject(DefaultChangeRequestStorageManager.CHANGE_REQUEST_XCLASS, 0, true, this.context))
            .thenReturn(xobject);

        this.storageManager.save(changeRequest);
        verify(changeRequest).setId("id42");
        verify(document).setTitle(title);
        verify(document).setContent(description);
        verify(document).setContentAuthorReference(userDocReference);
        verify(xobject).set("status", "draft", this.context);
        verify(this.fileChangeStorageManager).save(fileChange1);
        verify(this.fileChangeStorageManager).save(fileChange2);
        verify(this.wiki).saveDocument(document, this.context);
    }

    @Test
    void load() throws Exception
    {
        String id = "myId";
        ChangeRequest changeRequest = new ChangeRequest();
        changeRequest.setId(id);

        DocumentReference documentReference = mock(DocumentReference.class);

        when(this.changeRequestDocumentReferenceResolver.resolve(any())).thenAnswer(invocationOnMock -> {
            ChangeRequest actualChangeRequest = invocationOnMock.getArgument(0);
            assertEquals(id, actualChangeRequest.getId());
            return documentReference;
        });
        XWikiDocument document = mock(XWikiDocument.class);
        when(this.wiki.getDocument(documentReference, this.context)).thenReturn(document);
        BaseObject xobject = mock(BaseObject.class);
        when(document.getXObject(DefaultChangeRequestStorageManager.CHANGE_REQUEST_XCLASS)).thenReturn(xobject);

        when(document.isNew()).thenReturn(true);
        assertEquals(Optional.empty(), this.storageManager.load(id));

        when(document.isNew()).thenReturn(false);
        when(document.getXObject(DefaultChangeRequestStorageManager.CHANGE_REQUEST_XCLASS)).thenReturn(null);
        assertEquals(Optional.empty(), this.storageManager.load(id));

        when(document.getXObject(DefaultChangeRequestStorageManager.CHANGE_REQUEST_XCLASS)).thenReturn(xobject);
        XWikiAttachment attachment1 = mock(XWikiAttachment.class);
        XWikiAttachment attachment2 = mock(XWikiAttachment.class);
        XWikiAttachment attachment3 = mock(XWikiAttachment.class);
        XWikiAttachment attachment4 = mock(XWikiAttachment.class);
        when(document.getAttachmentList()).thenReturn(
            Arrays.asList(attachment1, attachment2, attachment3, attachment4));

        when(attachment1.getFilename()).thenReturn("something.png");
        when(attachment2.getFilename()).thenReturn("filechange.xml");
        when(attachment3.getFilename()).thenReturn("otherfile.xml");
        when(attachment4.getFilename()).thenReturn("filechange2.xml");

        FileChange fileChange1 = mock(FileChange.class);
        FileChange fileChange2 = mock(FileChange.class);
        when(this.fileChangeStorageManager.load(any(), eq("filechange"))).thenReturn(Optional.of(fileChange1));
        when(this.fileChangeStorageManager.load(any(), eq("otherfile"))).thenReturn(Optional.empty());
        when(this.fileChangeStorageManager.load(any(), eq("filechange2"))).thenReturn(Optional.of(fileChange2));

        String title = "sometitle";
        String description = "a description";
        when(document.getTitle()).thenReturn(title);
        when(document.getContent()).thenReturn(description);

        DocumentReference userDocReference = mock(DocumentReference.class);
        when(document.getContentAuthorReference()).thenReturn(userDocReference);

        UserReference userReference = mock(UserReference.class);
        when(this.userReferenceResolver.resolve(userDocReference)).thenReturn(userReference);

        when(xobject.getStringValue("status")).thenReturn("merged");
        when(document.getCreationDate()).thenReturn(new Date(42));

        changeRequest.setFileChanges(new ArrayList<>(Arrays.asList(fileChange1, fileChange2)))
            .setId(id)
            .setStatus(ChangeRequestStatus.MERGED)
            .setCreator(userReference)
            .setTitle(title)
            .setDescription(description)
            .setCreationDate(new Date(42));
        assertEquals(Optional.of(changeRequest), this.storageManager.load(id));
    }
}
