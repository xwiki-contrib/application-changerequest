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

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestConfiguration;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestStatus;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.internal.UserReferenceConverter;
import org.xwiki.contrib.changerequest.internal.cache.ChangeRequestStorageCacheManager;
import org.xwiki.contrib.changerequest.internal.id.ChangeRequestIDGenerator;
import org.xwiki.contrib.changerequest.storage.FileChangeStorageManager;
import org.xwiki.model.document.DocumentAuthors;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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
    @Named("title")
    private ChangeRequestIDGenerator idGenerator;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @MockComponent
    @Named("compactwiki")
    private EntityReferenceSerializer<String> localEntityReferenceSerializer;

    @MockComponent
    private ChangeRequestConfiguration configuration;

    @MockComponent
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @MockComponent
    private ChangeRequestStorageCacheManager changeRequestStorageCacheManager;

    @MockComponent
    @Named("document")
    private UserReferenceResolver<DocumentReference> userReferenceResolver;

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
        when(changeRequest.getAllFileChanges()).thenReturn(Arrays.asList(fileChange1, fileChange2));

        UserReference userReference = mock(UserReference.class);
        when(changeRequest.getCreator()).thenReturn(userReference);

        when(this.idGenerator.generateId(changeRequest)).thenReturn("id42");
        DocumentReference documentReference = mock(DocumentReference.class);
        when(this.changeRequestDocumentReferenceResolver.resolve(changeRequest)).thenReturn(documentReference);
        XWikiDocument document = mock(XWikiDocument.class);
        when(this.wiki.getDocument(documentReference, this.context)).thenReturn(document);
        when(document.isNew()).thenReturn(true);
        DocumentAuthors documentAuthors = mock(DocumentAuthors.class);
        when(document.getAuthors()).thenReturn(documentAuthors);

        DocumentReference userDocRef = mock(DocumentReference.class);
        when(context.getUserReference()).thenReturn(userDocRef);
        when(this.userReferenceResolver.resolve(userDocRef)).thenReturn(userReference);

        BaseObject xobject = mock(BaseObject.class);
        when(document.getXObject(ChangeRequestXClassInitializer.CHANGE_REQUEST_XCLASS, 0, true, this.context))
            .thenReturn(xobject);
        // First call when checking if the ID should be set, second call to invalidate the cache.
        when(changeRequest.getId()).thenReturn(null).thenReturn("id42");
        this.storageManager.save(changeRequest);
        verify(changeRequest).setId("id42");
        verify(document).setTitle(title);
        verify(document).setContent(description);
        verify(documentAuthors).setCreator(userReference);
        verify(documentAuthors).setOriginalMetadataAuthor(userReference);
        verify(xobject).set("status", "draft", this.context);
        verify(this.fileChangeStorageManager).save(fileChange1);
        verify(this.fileChangeStorageManager).save(fileChange2);
        verify(this.wiki).saveDocument(document, this.context);
        verify(this.changeRequestStorageCacheManager).invalidate("id42");
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
        when(document.getXObject(ChangeRequestXClassInitializer.CHANGE_REQUEST_XCLASS)).thenReturn(xobject);

        when(document.isNew()).thenReturn(true);
        assertEquals(Optional.empty(), this.storageManager.load(id));

        when(document.isNew()).thenReturn(false);
        when(document.getXObject(ChangeRequestXClassInitializer.CHANGE_REQUEST_XCLASS)).thenReturn(null);
        assertEquals(Optional.empty(), this.storageManager.load(id));

        when(document.getXObject(ChangeRequestXClassInitializer.CHANGE_REQUEST_XCLASS)).thenReturn(xobject);
        when(xobject.getListValue(ChangeRequestXClassInitializer.CHANGED_DOCUMENTS_FIELD))
            .thenReturn(Arrays.asList("ref1", "ref2"));

        DocumentReference ref1 = mock(DocumentReference.class);
        DocumentReference ref2 = mock(DocumentReference.class);
        when(this.documentReferenceResolver.resolve("ref1")).thenReturn(ref1);
        when(this.documentReferenceResolver.resolve("ref2")).thenReturn(ref2);

        FileChange fileChange1 = mock(FileChange.class);
        FileChange fileChange2 = mock(FileChange.class);
        FileChange fileChange3 = mock(FileChange.class);
        when(this.fileChangeStorageManager.load(any(), eq(ref1))).thenReturn(Arrays.asList(fileChange1, fileChange2));
        when(this.fileChangeStorageManager.load(any(), eq(ref2))).thenReturn(Collections.singletonList(fileChange3));

        when(fileChange1.getTargetEntity()).thenReturn(ref1);
        when(fileChange2.getTargetEntity()).thenReturn(ref1);
        when(fileChange3.getTargetEntity()).thenReturn(ref2);

        String title = "sometitle";
        String description = "a description";
        when(document.getTitle()).thenReturn(title);
        when(document.getContent()).thenReturn(description);

        UserReference userReference = mock(UserReference.class);
        DocumentAuthors documentAuthors = mock(DocumentAuthors.class);
        when(document.getAuthors()).thenReturn(documentAuthors);
        when(documentAuthors.getCreator()).thenReturn(userReference);

        when(xobject.getStringValue("status")).thenReturn("merged");
        when(document.getCreationDate()).thenReturn(new Date(42));

        changeRequest.addFileChange(fileChange1)
            .addFileChange(fileChange2)
            .addFileChange(fileChange3)
            .setId(id)
            .setStatus(ChangeRequestStatus.MERGED)
            .setCreator(userReference)
            .setTitle(title)
            .setDescription(description)
            .setCreationDate(new Date(42));
        assertEquals(Optional.of(changeRequest), this.storageManager.load(id));
        verify(this.changeRequestStorageCacheManager, times(3)).getChangeRequest(id);
    }

    @Test
    void findChangeRequestTargetingDocument() throws Exception
    {
        DocumentReference targetReference = mock(DocumentReference.class);
        when(this.localEntityReferenceSerializer.serialize(targetReference)).thenReturn("Foo.MyPage");

        when(this.entityReferenceSerializer.serialize(ChangeRequestXClassInitializer.CHANGE_REQUEST_XCLASS))
            .thenReturn("ChangeRequest.ChangeRequestClass");
        String expectedStatement =
            "from doc.object(ChangeRequest.ChangeRequestClass) as obj where :reference member of obj.changedDocuments";
        Query query = mock(Query.class);
        when(queryManager.createQuery(expectedStatement, Query.XWQL)).thenReturn(query);


        when(query.execute()).thenReturn(Arrays.asList("Space1.ref1", "Space2.ref2", "Space3.ref3"));
        DocumentReference ref1 = new DocumentReference("xwiki", "Space1", "ref1");
        DocumentReference ref2 = new DocumentReference("xwiki", "Space2", "ref2");
        DocumentReference ref3 = new DocumentReference("xwiki", "Space3", "ref3");

        when(this.documentReferenceResolver.resolve("Space1.ref1")).thenReturn(ref1);
        when(this.documentReferenceResolver.resolve("Space2.ref2")).thenReturn(ref2);
        when(this.documentReferenceResolver.resolve("Space3.ref3")).thenReturn(ref3);

        when(this.changeRequestDocumentReferenceResolver.resolve(any())).thenAnswer(invocationOnMock -> {
            ChangeRequest actualChangeRequest = invocationOnMock.getArgument(0);
            switch (actualChangeRequest.getId()) {
                case "Space1":
                    return ref1;
                case "Space2":
                    return ref2;
                case "Space3":
                    return ref3;
                default:
                    fail(String.format("Wrong change request id: [%s]", actualChangeRequest.getId()));
                    break;
            }
            return null;
        });

        // skip ref1
        XWikiDocument doc1 = mock(XWikiDocument.class);
        when(this.wiki.getDocument(ref1, this.context)).thenReturn(doc1);
        when(doc1.getXObject(ChangeRequestXClassInitializer.CHANGE_REQUEST_XCLASS)).thenReturn(null);

        // ref2
        XWikiDocument doc2 = mock(XWikiDocument.class);
        when(this.wiki.getDocument(ref2, this.context)).thenReturn(doc2);
        BaseObject xobject = mock(BaseObject.class);
        when(doc2.getXObject(ChangeRequestXClassInitializer.CHANGE_REQUEST_XCLASS)).thenReturn(xobject);
        String title = "sometitle";
        String description = "a description";
        when(doc2.getTitle()).thenReturn(title);
        when(doc2.getContent()).thenReturn(description);

        UserReference userReference = mock(UserReference.class);
        DocumentAuthors documentAuthors = mock(DocumentAuthors.class);
        when(doc2.getAuthors()).thenReturn(documentAuthors);
        when(documentAuthors.getCreator()).thenReturn(userReference);

        when(xobject.getStringValue("status")).thenReturn("merged");
        when(doc2.getCreationDate()).thenReturn(new Date(42));
        ChangeRequest cr2 = new ChangeRequest();
        cr2.setId("Space2")
            .setStatus(ChangeRequestStatus.MERGED)
            .setCreator(userReference)
            .setTitle(title)
            .setDescription(description)
            .setCreationDate(new Date(42));

        // ref3
        XWikiDocument doc3 = mock(XWikiDocument.class);
        when(this.wiki.getDocument(ref3, this.context)).thenReturn(doc3);
        BaseObject xobject2 = mock(BaseObject.class);
        when(doc3.getXObject(ChangeRequestXClassInitializer.CHANGE_REQUEST_XCLASS)).thenReturn(xobject2);
        String title2 = "anothertitle";
        String description2 = "another description";
        when(doc3.getTitle()).thenReturn(title2);
        when(doc3.getContent()).thenReturn(description2);

        UserReference userReference2 = mock(UserReference.class);
        DocumentAuthors documentAuthors2 = mock(DocumentAuthors.class);
        when(doc3.getAuthors()).thenReturn(documentAuthors2);
        when(documentAuthors2.getCreator()).thenReturn(userReference2);

        when(xobject2.getStringValue("status")).thenReturn("draft");
        when(doc3.getCreationDate()).thenReturn(new Date(16));
        ChangeRequest cr3 = new ChangeRequest();
        cr3.setId("Space3")
            .setStatus(ChangeRequestStatus.DRAFT)
            .setCreator(userReference2)
            .setTitle(title2)
            .setDescription(description2)
            .setCreationDate(new Date(16));

        assertEquals(Arrays.asList(cr2, cr3), this.storageManager.findChangeRequestTargeting(targetReference));
        verify(query).bindValue("reference", "Foo.MyPage");
    }

    @Test
    void findChangeRequestTargetingSpace() throws Exception
    {
        SpaceReference targetReference = mock(SpaceReference.class);
        when(this.localEntityReferenceSerializer.serialize(targetReference)).thenReturn("Foo.MySpace");

        when(this.entityReferenceSerializer.serialize(ChangeRequestXClassInitializer.CHANGE_REQUEST_XCLASS))
            .thenReturn("ChangeRequest.ChangeRequestClass");
        String expectedStatement = ", BaseObject as obj, DBStringListProperty as prop join prop.list list "
            + "where obj.name=doc.fullName and obj.className='ChangeRequest.ChangeRequestClass' and obj.id=prop.id.id "
            + "and prop.id.name='changedDocuments' and list like :reference order by doc.creationDate desc";
        Query query = mock(Query.class);
        when(queryManager.createQuery(expectedStatement, Query.HQL)).thenReturn(query);

        when(query.execute()).thenReturn(Arrays.asList("Space1.ref1", "Space2.ref2", "Space3.ref3"));
        DocumentReference ref1 = new DocumentReference("xwiki", "Space1", "ref1");
        DocumentReference ref2 = new DocumentReference("xwiki", "Space2", "ref2");
        DocumentReference ref3 = new DocumentReference("xwiki", "Space3", "ref3");

        when(this.documentReferenceResolver.resolve("Space1.ref1")).thenReturn(ref1);
        when(this.documentReferenceResolver.resolve("Space2.ref2")).thenReturn(ref2);
        when(this.documentReferenceResolver.resolve("Space3.ref3")).thenReturn(ref3);

        when(this.changeRequestDocumentReferenceResolver.resolve(any())).thenAnswer(invocationOnMock -> {
            ChangeRequest actualChangeRequest = invocationOnMock.getArgument(0);
            switch (actualChangeRequest.getId()) {
                case "Space1":
                    return ref1;
                case "Space2":
                    return ref2;
                case "Space3":
                    return ref3;
                default:
                    fail(String.format("Wrong change request id: [%s]", actualChangeRequest.getId()));
                    break;
            }
            return null;
        });

        // skip ref1
        XWikiDocument doc1 = mock(XWikiDocument.class);
        when(this.wiki.getDocument(ref1, this.context)).thenReturn(doc1);
        when(doc1.getXObject(ChangeRequestXClassInitializer.CHANGE_REQUEST_XCLASS)).thenReturn(null);

        // ref2
        XWikiDocument doc2 = mock(XWikiDocument.class);
        when(this.wiki.getDocument(ref2, this.context)).thenReturn(doc2);
        BaseObject xobject = mock(BaseObject.class);
        when(doc2.getXObject(ChangeRequestXClassInitializer.CHANGE_REQUEST_XCLASS)).thenReturn(xobject);
        String title = "sometitle";
        String description = "a description";
        when(doc2.getTitle()).thenReturn(title);
        when(doc2.getContent()).thenReturn(description);

        UserReference userReference = mock(UserReference.class);
        DocumentAuthors documentAuthors = mock(DocumentAuthors.class);
        when(doc2.getAuthors()).thenReturn(documentAuthors);
        when(documentAuthors.getCreator()).thenReturn(userReference);

        when(xobject.getStringValue("status")).thenReturn("merged");
        when(doc2.getCreationDate()).thenReturn(new Date(42));
        ChangeRequest cr2 = new ChangeRequest();
        cr2.setId("Space2")
            .setStatus(ChangeRequestStatus.MERGED)
            .setCreator(userReference)
            .setTitle(title)
            .setDescription(description)
            .setCreationDate(new Date(42));

        // ref3
        XWikiDocument doc3 = mock(XWikiDocument.class);
        when(this.wiki.getDocument(ref3, this.context)).thenReturn(doc3);
        BaseObject xobject2 = mock(BaseObject.class);
        when(doc3.getXObject(ChangeRequestXClassInitializer.CHANGE_REQUEST_XCLASS)).thenReturn(xobject2);
        String title2 = "anothertitle";
        String description2 = "another description";
        when(doc3.getTitle()).thenReturn(title2);
        when(doc3.getContent()).thenReturn(description2);

        UserReference userReference2 = mock(UserReference.class);
        DocumentAuthors documentAuthors2 = mock(DocumentAuthors.class);
        when(doc3.getAuthors()).thenReturn(documentAuthors2);
        when(documentAuthors2.getCreator()).thenReturn(userReference2);

        when(xobject2.getStringValue("status")).thenReturn("draft");
        when(doc3.getCreationDate()).thenReturn(new Date(16));
        ChangeRequest cr3 = new ChangeRequest();
        cr3.setId("Space3")
            .setStatus(ChangeRequestStatus.DRAFT)
            .setCreator(userReference2)
            .setTitle(title2)
            .setDescription(description2)
            .setCreationDate(new Date(16));

        assertEquals(Arrays.asList(cr2, cr3), this.storageManager.findChangeRequestTargeting(targetReference));
        verify(query).bindValue("reference", "%Foo.MySpace%");
    }

    @Test
    void saveStaleDate() throws XWikiException, ChangeRequestException
    {
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        ChangeRequestException changeRequestException =
            assertThrows(ChangeRequestException.class, () -> this.storageManager.saveStaleDate(changeRequest));
        assertEquals("The stale date can only be saved for existing change requests.",
            changeRequestException.getMessage());

        String id = "someId";
        when(changeRequest.getId()).thenReturn(id);
        DocumentReference documentReference = mock(DocumentReference.class);
        when(this.changeRequestDocumentReferenceResolver.resolve(changeRequest)).thenReturn(documentReference);
        XWikiDocument document = mock(XWikiDocument.class);
        when(this.wiki.getDocument(documentReference, this.context)).thenReturn(document);
        BaseObject baseObject = mock(BaseObject.class);
        when(document.getXObject(ChangeRequestXClassInitializer.CHANGE_REQUEST_XCLASS, 0, true, context))
            .thenReturn(baseObject);
        Date date = new Date(845);
        when(changeRequest.getStaleDate()).thenReturn(date);
        this.storageManager.saveStaleDate(changeRequest);
        verify(baseObject).set(ChangeRequestXClassInitializer.STALE_DATE_FIELD, date, this.context);
        verify(wiki).saveDocument(document, this.context);
    }
}
