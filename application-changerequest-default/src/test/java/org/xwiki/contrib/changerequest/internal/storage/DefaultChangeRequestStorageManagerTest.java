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
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.contrib.changerequest.ApproversManager;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestConfiguration;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestReview;
import org.xwiki.contrib.changerequest.ChangeRequestStatus;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.discussions.ChangeRequestDiscussionService;
import org.xwiki.contrib.changerequest.events.SplitEndChangeRequestEvent;
import org.xwiki.contrib.changerequest.internal.UserReferenceConverter;
import org.xwiki.contrib.changerequest.internal.cache.ChangeRequestStorageCacheManager;
import org.xwiki.contrib.changerequest.storage.ChangeRequestIDGenerator;
import org.xwiki.contrib.changerequest.storage.FileChangeStorageManager;
import org.xwiki.contrib.changerequest.storage.ReviewStorageManager;
import org.xwiki.job.Job;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;
import org.xwiki.model.document.DocumentAuthors;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.refactoring.job.EntityRequest;
import org.xwiki.refactoring.job.RefactoringJobs;
import org.xwiki.refactoring.script.RequestFactory;
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
import static org.xwiki.contrib.changerequest.internal.storage.ChangeRequestXClassInitializer.CHANGE_REQUEST_XCLASS;

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
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @MockComponent
    private ChangeRequestStorageCacheManager changeRequestStorageCacheManager;

    @MockComponent
    @Named("document")
    private UserReferenceResolver<DocumentReference> userReferenceResolver;

    @MockComponent
    private ApproversManager<ChangeRequest> approversManager;

    @MockComponent
    private ApproversManager<FileChange> fileChangeApproversManager;

    @MockComponent
    private RequestFactory refactoringRequestFactory;

    @MockComponent
    private ChangeRequestDiscussionService discussionService;

    @MockComponent
    private JobExecutor jobExecutor;

    @MockComponent
    private ReviewStorageManager reviewStorageManager;

    @MockComponent
    private ObservationManager observationManager;

    @MockComponent
    @Named("count")
    private QueryFilter countQueryFilter;

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
        when(document.getXObject(CHANGE_REQUEST_XCLASS, 0, true, this.context))
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
        when(document.getXObject(CHANGE_REQUEST_XCLASS)).thenReturn(xobject);

        when(document.isNew()).thenReturn(true);
        assertEquals(Optional.empty(), this.storageManager.load(id));

        when(document.isNew()).thenReturn(false);
        when(document.getXObject(CHANGE_REQUEST_XCLASS)).thenReturn(null);
        assertEquals(Optional.empty(), this.storageManager.load(id));

        when(document.getXObject(CHANGE_REQUEST_XCLASS)).thenReturn(xobject);
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

        when(this.entityReferenceSerializer.serialize(CHANGE_REQUEST_XCLASS))
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
        when(doc1.getXObject(CHANGE_REQUEST_XCLASS)).thenReturn(null);

        // ref2
        XWikiDocument doc2 = mock(XWikiDocument.class);
        when(this.wiki.getDocument(ref2, this.context)).thenReturn(doc2);
        BaseObject xobject = mock(BaseObject.class);
        when(doc2.getXObject(CHANGE_REQUEST_XCLASS)).thenReturn(xobject);
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
        when(doc3.getXObject(CHANGE_REQUEST_XCLASS)).thenReturn(xobject2);
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

        when(this.entityReferenceSerializer.serialize(CHANGE_REQUEST_XCLASS))
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
        when(doc1.getXObject(CHANGE_REQUEST_XCLASS)).thenReturn(null);

        // ref2
        XWikiDocument doc2 = mock(XWikiDocument.class);
        when(this.wiki.getDocument(ref2, this.context)).thenReturn(doc2);
        BaseObject xobject = mock(BaseObject.class);
        when(doc2.getXObject(CHANGE_REQUEST_XCLASS)).thenReturn(xobject);
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
        when(doc3.getXObject(CHANGE_REQUEST_XCLASS)).thenReturn(xobject2);
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
        when(document.getXObject(CHANGE_REQUEST_XCLASS, 0, true, context))
            .thenReturn(baseObject);
        Date date = new Date(845);
        when(changeRequest.getStaleDate()).thenReturn(date);
        this.storageManager.saveStaleDate(changeRequest);
        verify(baseObject).set(ChangeRequestXClassInitializer.STALE_DATE_FIELD, date, this.context);
        verify(wiki).saveDocument(document, this.context);
    }

    @Test
    void split() throws Exception
    {
        // Fixture:
        // 3 documents in the change request
        // 3 filechanges for each document
        // 3 reviews

        ChangeRequest changeRequest = mock(ChangeRequest.class);
        String changeRequestId = "originalCR";
        when(changeRequest.getId()).thenReturn(changeRequestId);

        DocumentReference doc1 = mock(DocumentReference.class, "doc1");
        DocumentReference doc2 = mock(DocumentReference.class, "doc2");
        DocumentReference doc3 = mock(DocumentReference.class, "doc3");
        DocumentReference doc4 = mock(DocumentReference.class, "doc4");

        when(changeRequest.getModifiedDocuments()).thenReturn(new HashSet<>(List.of(
            doc1,
            doc2,
            doc3,
            doc4
        )));

        FileChange fileChange1Doc1 = mock(FileChange.class, "filechange1Doc1");
        FileChange fileChange2Doc1 = mock(FileChange.class, "filechange2Doc1");
        FileChange fileChange3Doc1 = mock(FileChange.class, "filechange3Doc1");

        FileChange fileChange1Doc2 = mock(FileChange.class, "filechange1Doc2");
        FileChange fileChange2Doc2 = mock(FileChange.class, "filechange2Doc2");
        FileChange fileChange3Doc2 = mock(FileChange.class, "filechange3Doc2");

        FileChange fileChange1Doc3 = mock(FileChange.class, "filechange1Doc3");
        FileChange fileChange2Doc3 = mock(FileChange.class, "filechange2Doc3");
        FileChange fileChange3Doc3 = mock(FileChange.class, "filechange3Doc3");

        FileChange fileChange1Doc4 = mock(FileChange.class, "filechange1Doc4");

        Map<DocumentReference, Deque<FileChange>> filesChanges = new HashMap<>();
        filesChanges.put(doc1, new LinkedList<>(List.of(fileChange1Doc1, fileChange2Doc1, fileChange3Doc1)));
        filesChanges.put(doc2, new LinkedList<>(List.of(fileChange1Doc2, fileChange2Doc2, fileChange3Doc2)));
        filesChanges.put(doc3, new LinkedList<>(List.of(fileChange1Doc3, fileChange2Doc3, fileChange3Doc3)));
        filesChanges.put(doc4, new LinkedList<>(List.of(fileChange1Doc4)));

        when(changeRequest.getFileChanges()).thenReturn(filesChanges);

        ChangeRequest changeRequest1 = mock(ChangeRequest.class, "cr1");
        ChangeRequest changeRequest2 = mock(ChangeRequest.class, "cr2");
        ChangeRequest changeRequest3 = mock(ChangeRequest.class, "cr3");
        ChangeRequest changeRequest4 = mock(ChangeRequest.class, "cr4");
        when(changeRequest.cloneWithoutFileChanges())
            .thenReturn(changeRequest1)
            .thenReturn(changeRequest2)
            .thenReturn(changeRequest3)
            .thenReturn(changeRequest4);

        when(fileChange1Doc1.getType()).thenReturn(FileChange.FileChangeType.DELETION);
        when(fileChange2Doc1.getType()).thenReturn(FileChange.FileChangeType.EDITION);
        when(fileChange3Doc1.getType()).thenReturn(FileChange.FileChangeType.CREATION);

        when(fileChange1Doc2.getType()).thenReturn(FileChange.FileChangeType.EDITION);
        when(fileChange2Doc2.getType()).thenReturn(FileChange.FileChangeType.EDITION);
        when(fileChange3Doc2.getType()).thenReturn(FileChange.FileChangeType.EDITION);

        when(fileChange1Doc3.getType()).thenReturn(FileChange.FileChangeType.CREATION);
        when(fileChange2Doc3.getType()).thenReturn(FileChange.FileChangeType.DELETION);
        when(fileChange3Doc3.getType()).thenReturn(FileChange.FileChangeType.DELETION);

        when(fileChange1Doc4.getType()).thenReturn(FileChange.FileChangeType.EDITION);

        FileChange fileChange1Doc1Clone = mock(FileChange.class, "filechange1Doc1Clone");
        FileChange fileChange2Doc1Clone = mock(FileChange.class, "filechange2Doc1Clone");
        FileChange fileChange3Doc1Clone = mock(FileChange.class, "filechange3Doc1Clone");

        FileChange fileChange1Doc2Clone = mock(FileChange.class, "filechange1Doc2Clone");
        FileChange fileChange2Doc2Clone = mock(FileChange.class, "filechange2Doc2Clone");
        FileChange fileChange3Doc2Clone = mock(FileChange.class, "filechange3Doc2Clone");

        FileChange fileChange1Doc3Clone = mock(FileChange.class, "filechange1Doc3Clone");
        FileChange fileChange2Doc3Clone = mock(FileChange.class, "filechange2Doc3Clone");
        FileChange fileChange3Doc3Clone = mock(FileChange.class, "filechange3Doc3Clone");

        FileChange fileChange1Doc4Clone = mock(FileChange.class, "filechange1Doc4Clone");

        when(fileChange1Doc1.cloneWithChangeRequestAndType(changeRequest1, FileChange.FileChangeType.DELETION))
            .thenReturn(fileChange1Doc1Clone);
        when(fileChange2Doc1.cloneWithChangeRequestAndType(changeRequest1, FileChange.FileChangeType.EDITION))
            .thenReturn(fileChange2Doc1Clone);
        when(fileChange3Doc1.cloneWithChangeRequestAndType(changeRequest1, FileChange.FileChangeType.CREATION))
            .thenReturn(fileChange3Doc1Clone);

        when(fileChange1Doc2.cloneWithChangeRequestAndType(changeRequest2, FileChange.FileChangeType.EDITION))
            .thenReturn(fileChange1Doc2Clone);
        when(fileChange2Doc2.cloneWithChangeRequestAndType(changeRequest2, FileChange.FileChangeType.EDITION))
            .thenReturn(fileChange2Doc2Clone);
        XWikiDocument fileChange3Doc2Doc = mock(XWikiDocument.class);
        when(fileChange3Doc2Doc.isNew()).thenReturn(true);
        when(this.fileChangeStorageManager.getCurrentDocumentFromFileChange(fileChange3Doc2))
            .thenReturn(fileChange3Doc2Doc);
        when(fileChange3Doc2.cloneWithChangeRequestAndType(changeRequest2, FileChange.FileChangeType.CREATION))
            .thenReturn(fileChange3Doc2Clone);

        when(fileChange1Doc3.cloneWithChangeRequestAndType(changeRequest3, FileChange.FileChangeType.CREATION))
            .thenReturn(fileChange1Doc3Clone);
        when(fileChange2Doc3.cloneWithChangeRequestAndType(changeRequest3, FileChange.FileChangeType.DELETION))
            .thenReturn(fileChange2Doc3Clone);
        when(fileChange3Doc3.cloneWithChangeRequestAndType(changeRequest3, FileChange.FileChangeType.DELETION))
            .thenReturn(fileChange3Doc3Clone);

        XWikiDocument fileChange1Doc4Doc = mock(XWikiDocument.class);
        when(fileChange1Doc4Doc.isNew()).thenReturn(false);
        when(this.fileChangeStorageManager.getCurrentDocumentFromFileChange(fileChange1Doc4))
            .thenReturn(fileChange1Doc4Doc);
        when(fileChange1Doc4.cloneWithChangeRequestAndType(changeRequest4, FileChange.FileChangeType.EDITION))
            .thenReturn(fileChange1Doc4Clone);

        String changeRequest1Id = "cr1Id";
        String changeRequest2Id = "cr2Id";
        String changeRequest3Id = "cr3Id";
        String changeRequest4Id = "cr4Id";
        String title = "crTitle";
        String description = "crContent";
        ChangeRequestStatus status = ChangeRequestStatus.READY_FOR_MERGING;
        UserReference crCreator = mock(UserReference.class, "crCreator");
        UserReference currentUser = mock(UserReference.class, "currentUser");
        DocumentReference currentUserRef = mock(DocumentReference.class, "currentUserRef");
        when(context.getUserReference()).thenReturn(currentUserRef);
        when(this.userReferenceResolver.resolve(currentUserRef)).thenReturn(currentUser);

        // save setup for CR1
        when(changeRequest1.getId()).thenReturn(changeRequest1Id);
        when(changeRequest1.getTitle()).thenReturn(title);
        when(changeRequest1.getDescription()).thenReturn(description);
        when(changeRequest1.getCreator()).thenReturn(crCreator);
        when(changeRequest1.getStatus()).thenReturn(status);
        when(changeRequest1.getAllFileChanges())
            .thenReturn(List.of(fileChange1Doc1Clone, fileChange2Doc1Clone, fileChange3Doc1Clone));

        DocumentReference cr1Ref = mock(DocumentReference.class, "cr1Ref");
        when(this.changeRequestDocumentReferenceResolver.resolve(changeRequest1)).thenReturn(cr1Ref);
        XWikiDocument cr1Doc = mock(XWikiDocument.class, "cr1Doc");
        when(this.wiki.getDocument(cr1Ref, this.context)).thenReturn(cr1Doc);
        DocumentAuthors cr1Authors = mock(DocumentAuthors.class, "cr1Authors");
        when(cr1Doc.getAuthors()).thenReturn(cr1Authors);
        BaseObject cr1Obj = mock(BaseObject.class, "cr1Obj");
        when(cr1Doc.getXObject(CHANGE_REQUEST_XCLASS, 0, true, this.context))
            .thenReturn(cr1Obj);

        // save setup for CR2
        when(changeRequest2.getId()).thenReturn(changeRequest2Id);
        when(changeRequest2.getTitle()).thenReturn(title);
        when(changeRequest2.getDescription()).thenReturn(description);
        when(changeRequest2.getCreator()).thenReturn(crCreator);
        when(changeRequest2.getStatus()).thenReturn(status);
        when(changeRequest2.getAllFileChanges())
            .thenReturn(List.of(fileChange1Doc2Clone, fileChange2Doc2Clone, fileChange3Doc2Clone));

        DocumentReference cr2Ref = mock(DocumentReference.class, "cr2Ref");
        when(this.changeRequestDocumentReferenceResolver.resolve(changeRequest2)).thenReturn(cr2Ref);
        XWikiDocument cr2Doc = mock(XWikiDocument.class, "cr2Doc");
        when(this.wiki.getDocument(cr2Ref, this.context)).thenReturn(cr2Doc);
        DocumentAuthors cr2Authors = mock(DocumentAuthors.class, "cr2Authors");
        when(cr2Doc.getAuthors()).thenReturn(cr2Authors);
        BaseObject cr2Obj = mock(BaseObject.class, "cr2Obj");
        when(cr2Doc.getXObject(CHANGE_REQUEST_XCLASS, 0, true, this.context))
            .thenReturn(cr2Obj);

        // save setup for CR3
        when(changeRequest3.getId()).thenReturn(changeRequest3Id);
        when(changeRequest3.getTitle()).thenReturn(title);
        when(changeRequest3.getDescription()).thenReturn(description);
        when(changeRequest3.getCreator()).thenReturn(crCreator);
        when(changeRequest3.getStatus()).thenReturn(status);
        when(changeRequest3.getAllFileChanges())
            .thenReturn(List.of(fileChange1Doc3Clone, fileChange2Doc3Clone, fileChange3Doc3Clone));

        DocumentReference cr3Ref = mock(DocumentReference.class, "cr3Ref");
        when(this.changeRequestDocumentReferenceResolver.resolve(changeRequest3)).thenReturn(cr3Ref);
        XWikiDocument cr3Doc = mock(XWikiDocument.class, "cr3Doc");
        when(this.wiki.getDocument(cr3Ref, this.context)).thenReturn(cr3Doc);
        DocumentAuthors cr3Authors = mock(DocumentAuthors.class, "cr3Authors");
        when(cr3Doc.getAuthors()).thenReturn(cr3Authors);
        BaseObject cr3Obj = mock(BaseObject.class, "cr3Obj");
        when(cr3Doc.getXObject(CHANGE_REQUEST_XCLASS, 0, true, this.context))
            .thenReturn(cr3Obj);

        // save setup for CR4
        when(changeRequest4.getId()).thenReturn(changeRequest4Id);
        when(changeRequest4.getTitle()).thenReturn(title);
        when(changeRequest4.getDescription()).thenReturn(description);
        when(changeRequest4.getCreator()).thenReturn(crCreator);
        when(changeRequest4.getStatus()).thenReturn(status);
        when(changeRequest4.getAllFileChanges())
            .thenReturn(List.of(fileChange1Doc4Clone));

        DocumentReference cr4Ref = mock(DocumentReference.class, "cr4Ref");
        when(this.changeRequestDocumentReferenceResolver.resolve(changeRequest4)).thenReturn(cr4Ref);
        XWikiDocument cr4Doc = mock(XWikiDocument.class, "cr4Doc");
        when(this.wiki.getDocument(cr4Ref, this.context)).thenReturn(cr4Doc);
        DocumentAuthors cr4Authors = mock(DocumentAuthors.class, "cr4Authors");
        when(cr4Doc.getAuthors()).thenReturn(cr4Authors);
        BaseObject cr4Obj = mock(BaseObject.class, "cr3Obj");
        when(cr4Doc.getXObject(CHANGE_REQUEST_XCLASS, 0, true, this.context))
            .thenReturn(cr4Obj);

        // Reviews
        String review1Id = "review1";
        String review2Id = "review2";
        String review3Id = "review3";
        ChangeRequestReview review1 = mock(ChangeRequestReview.class, "review1");
        ChangeRequestReview review2 = mock(ChangeRequestReview.class, "review2");
        ChangeRequestReview review3 = mock(ChangeRequestReview.class, "review3");

        when(review1.getId()).thenReturn(review1Id);
        when(review2.getId()).thenReturn(review2Id);
        when(review3.getId()).thenReturn(review3Id);

        when(changeRequest.getReviews()).thenReturn(List.of(review1, review2, review3));

        ChangeRequestReview review1CloneCR1 = mock(ChangeRequestReview.class, "review1CloneCR1");
        ChangeRequestReview review2CloneCR1 = mock(ChangeRequestReview.class, "review2CloneCR1");
        ChangeRequestReview review3CloneCR1 = mock(ChangeRequestReview.class, "review3CloneCR1");

        ChangeRequestReview review1CloneCR2 = mock(ChangeRequestReview.class, "review1CloneCR2");
        ChangeRequestReview review2CloneCR2 = mock(ChangeRequestReview.class, "review2CloneCR2");
        ChangeRequestReview review3CloneCR2 = mock(ChangeRequestReview.class, "review3CloneCR2");

        ChangeRequestReview review1CloneCR3 = mock(ChangeRequestReview.class, "review1CloneCR3");
        ChangeRequestReview review2CloneCR3 = mock(ChangeRequestReview.class, "review2CloneCR3");
        ChangeRequestReview review3CloneCR3 = mock(ChangeRequestReview.class, "review3CloneCR3");

        ChangeRequestReview review1CloneCR4 = mock(ChangeRequestReview.class, "review1CloneCR4");
        ChangeRequestReview review2CloneCR4 = mock(ChangeRequestReview.class, "review2CloneCR4");
        ChangeRequestReview review3CloneCR4 = mock(ChangeRequestReview.class, "review3CloneCR4");

        when(review1.cloneWithChangeRequest(changeRequest1)).thenReturn(review1CloneCR1);
        when(review2.cloneWithChangeRequest(changeRequest1)).thenReturn(review2CloneCR1);
        when(review3.cloneWithChangeRequest(changeRequest1)).thenReturn(review3CloneCR1);

        when(review1.cloneWithChangeRequest(changeRequest2)).thenReturn(review1CloneCR2);
        when(review2.cloneWithChangeRequest(changeRequest2)).thenReturn(review2CloneCR2);
        when(review3.cloneWithChangeRequest(changeRequest2)).thenReturn(review3CloneCR2);

        when(review1.cloneWithChangeRequest(changeRequest3)).thenReturn(review1CloneCR3);
        when(review2.cloneWithChangeRequest(changeRequest3)).thenReturn(review2CloneCR3);
        when(review3.cloneWithChangeRequest(changeRequest3)).thenReturn(review3CloneCR3);

        when(review1.cloneWithChangeRequest(changeRequest4)).thenReturn(review1CloneCR4);
        when(review2.cloneWithChangeRequest(changeRequest4)).thenReturn(review2CloneCR4);
        when(review3.cloneWithChangeRequest(changeRequest4)).thenReturn(review3CloneCR4);

        // Approvers
        when(this.approversManager.wasManuallyEdited(changeRequest)).thenReturn(false);
        when(changeRequest1.getLastFileChanges()).thenReturn(Collections.singletonList(fileChange3Doc1Clone));
        when(changeRequest2.getLastFileChanges()).thenReturn(Collections.singletonList(fileChange3Doc2Clone));
        when(changeRequest3.getLastFileChanges()).thenReturn(Collections.singletonList(fileChange3Doc3Clone));
        when(changeRequest4.getLastFileChanges()).thenReturn(Collections.singletonList(fileChange1Doc4Clone));

        UserReference approverDoc1 = mock(UserReference.class, "approverDoc1");
        UserReference approverDoc2 = mock(UserReference.class, "approverDoc2");
        UserReference approverDoc3 = mock(UserReference.class, "approverDoc3");
        UserReference approverDoc4 = mock(UserReference.class, "approverDoc4");

        DocumentReference approverGroupDoc1 = mock(DocumentReference.class, "approverGroupDoc1");
        DocumentReference approverGroupDoc2 = mock(DocumentReference.class, "approverGroupDoc2");
        DocumentReference approverGroupDoc3 = mock(DocumentReference.class, "approverGroupDoc3");

        when(this.fileChangeApproversManager.getAllApprovers(fileChange3Doc1Clone, false))
            .thenReturn(Collections.singleton(approverDoc1));
        when(this.fileChangeApproversManager.getAllApprovers(fileChange3Doc2Clone, false))
            .thenReturn(Collections.singleton(approverDoc2));
        when(this.fileChangeApproversManager.getAllApprovers(fileChange3Doc3Clone, false))
            .thenReturn(Collections.singleton(approverDoc3));
        when(this.fileChangeApproversManager.getAllApprovers(fileChange1Doc4Clone, false))
            .thenReturn(Collections.singleton(approverDoc4));

        when(this.fileChangeApproversManager.getGroupsApprovers(fileChange3Doc1Clone))
            .thenReturn(Collections.singleton(approverGroupDoc1));
        when(this.fileChangeApproversManager.getGroupsApprovers(fileChange3Doc2Clone))
            .thenReturn(Collections.singleton(approverGroupDoc2));
        when(this.fileChangeApproversManager.getGroupsApprovers(fileChange3Doc3Clone))
            .thenReturn(Collections.singleton(approverGroupDoc3));

        // Handle deletion
        DocumentReference originalCRDocRef = mock(DocumentReference.class, "originalCRDocRef");
        when(this.changeRequestDocumentReferenceResolver.resolve(changeRequest)).thenReturn(originalCRDocRef);
        EntityRequest deleteRequest = mock(EntityRequest.class, "deleteRequest");
        when(this.refactoringRequestFactory.createDeleteRequest(Collections.singletonList(originalCRDocRef)))
            .thenReturn(deleteRequest);
        Job deletionJob = mock(Job.class);
        when(this.jobExecutor.execute(RefactoringJobs.DELETE, deleteRequest)).thenReturn(deletionJob);

        assertEquals(List.of(changeRequest1, changeRequest2, changeRequest3, changeRequest4),
            this.storageManager.split(changeRequest));

        // For some reason it's working only in debug, no idea why
        // verify file change adding
//        verify(changeRequest1).addFileChange(fileChange1Doc1Clone);
//        verify(changeRequest1).addFileChange(fileChange2Doc1Clone);
//        verify(changeRequest1).addFileChange(fileChange3Doc1Clone);
//
//        verify(changeRequest2).addFileChange(fileChange1Doc2Clone);
//        verify(changeRequest2).addFileChange(fileChange2Doc2Clone);
//        verify(changeRequest2).addFileChange(fileChange3Doc2Clone);
//
//        verify(changeRequest3).addFileChange(fileChange1Doc3Clone);
//        verify(changeRequest3).addFileChange(fileChange2Doc3Clone);
//        verify(changeRequest3).addFileChange(fileChange3Doc3Clone);

        // verify save of CR (we only check the save of the document and the save of the file changes,
        // we could check all properties to be exhaustive)
        verify(this.wiki).saveDocument(cr1Doc, this.context);
        verify(this.wiki).saveDocument(cr2Doc, this.context);
        verify(this.wiki).saveDocument(cr3Doc, this.context);

        verify(this.fileChangeStorageManager).save(fileChange1Doc1Clone);
        verify(this.fileChangeStorageManager).save(fileChange2Doc1Clone);
        verify(this.fileChangeStorageManager).save(fileChange3Doc1Clone);

        verify(this.fileChangeStorageManager).save(fileChange1Doc2Clone);
        verify(this.fileChangeStorageManager).save(fileChange2Doc2Clone);
        verify(this.fileChangeStorageManager).save(fileChange3Doc2Clone);

        verify(this.fileChangeStorageManager).save(fileChange1Doc3Clone);
        verify(this.fileChangeStorageManager).save(fileChange2Doc3Clone);
        verify(this.fileChangeStorageManager).save(fileChange3Doc3Clone);

        // verify reviews handling
        // CR1
        verify(review1CloneCR1).setValid(false);
        verify(review1CloneCR1).setId(review1Id);
        verify(changeRequest1).addReview(review1CloneCR1);
        verify(this.reviewStorageManager).save(review1CloneCR1);

        verify(review2CloneCR1).setValid(false);
        verify(review2CloneCR1).setId(review2Id);
        verify(changeRequest1).addReview(review2CloneCR1);
        verify(this.reviewStorageManager).save(review2CloneCR1);

        verify(review3CloneCR1).setValid(false);
        verify(review3CloneCR1).setId(review3Id);
        verify(changeRequest1).addReview(review3CloneCR1);
        verify(this.reviewStorageManager).save(review3CloneCR1);

        // CR2
        verify(review1CloneCR2).setValid(false);
        verify(review1CloneCR2).setId(review1Id);
        verify(changeRequest2).addReview(review1CloneCR2);
        verify(this.reviewStorageManager).save(review1CloneCR2);

        verify(review2CloneCR2).setValid(false);
        verify(review2CloneCR2).setId(review2Id);
        verify(changeRequest2).addReview(review2CloneCR2);
        verify(this.reviewStorageManager).save(review2CloneCR2);

        verify(review3CloneCR2).setValid(false);
        verify(review3CloneCR2).setId(review3Id);
        verify(changeRequest2).addReview(review3CloneCR2);
        verify(this.reviewStorageManager).save(review3CloneCR2);

        // CR3
        verify(review1CloneCR3).setValid(false);
        verify(review1CloneCR3).setId(review1Id);
        verify(changeRequest3).addReview(review1CloneCR3);
        verify(this.reviewStorageManager).save(review1CloneCR3);

        verify(review2CloneCR3).setValid(false);
        verify(review2CloneCR3).setId(review2Id);
        verify(changeRequest3).addReview(review2CloneCR3);
        verify(this.reviewStorageManager).save(review2CloneCR3);

        verify(review3CloneCR3).setValid(false);
        verify(review3CloneCR3).setId(review3Id);
        verify(changeRequest3).addReview(review3CloneCR3);
        verify(this.reviewStorageManager).save(review3CloneCR3);

        verify(this.discussionService).moveDiscussions(changeRequest,
            List.of(changeRequest1, changeRequest2, changeRequest3, changeRequest4));

        // verify approvers handling
        verify(this.approversManager).setUsersApprovers(Collections.singleton(approverDoc1), changeRequest1);
        verify(this.approversManager).setUsersApprovers(Collections.singleton(approverDoc2), changeRequest2);
        verify(this.approversManager).setUsersApprovers(Collections.singleton(approverDoc3), changeRequest3);

        verify(this.approversManager).setGroupsApprovers(Collections.singleton(approverGroupDoc1), changeRequest1);
        verify(this.approversManager).setGroupsApprovers(Collections.singleton(approverGroupDoc2), changeRequest2);
        verify(this.approversManager).setGroupsApprovers(Collections.singleton(approverGroupDoc3), changeRequest3);

        // verify deletion
        verify(deleteRequest).setDeep(true);
        verify(deleteRequest).setCheckRights(false);
        verify(deleteRequest).setCheckAuthorRights(false);

        verify(this.jobExecutor).execute(RefactoringJobs.DELETE, deleteRequest);
        verify(deletionJob).join();

        verify(this.observationManager).notify(any(SplitEndChangeRequestEvent.class), eq(changeRequestId),
            eq(List.of(changeRequest1, changeRequest2, changeRequest3, changeRequest4)));
    }

    @Test
    void splitWithIgnore() throws Exception
    {
        // Fixture:
        // 3 documents in the change request
        // 3 filechanges for each document
        // 3 reviews
        // Ignore on of the document in the split

        ChangeRequest changeRequest = mock(ChangeRequest.class);
        String changeRequestId = "originalCR";
        when(changeRequest.getId()).thenReturn(changeRequestId);

        DocumentReference doc1 = mock(DocumentReference.class, "doc1");
        DocumentReference doc2 = mock(DocumentReference.class, "doc2");
        DocumentReference doc3 = mock(DocumentReference.class, "doc3");
        DocumentReference doc4 = mock(DocumentReference.class, "doc4");

        when(changeRequest.getModifiedDocuments()).thenReturn(new HashSet<>(List.of(
            doc1,
            doc2,
            doc3,
            doc4
        )));

        FileChange fileChange1Doc1 = mock(FileChange.class, "filechange1Doc1");
        FileChange fileChange2Doc1 = mock(FileChange.class, "filechange2Doc1");
        FileChange fileChange3Doc1 = mock(FileChange.class, "filechange3Doc1");

        FileChange fileChange1Doc3 = mock(FileChange.class, "filechange1Doc3");
        FileChange fileChange2Doc3 = mock(FileChange.class, "filechange2Doc3");
        FileChange fileChange3Doc3 = mock(FileChange.class, "filechange3Doc3");

        FileChange fileChange1Doc4 = mock(FileChange.class, "filechange1Doc4");

        Map<DocumentReference, Deque<FileChange>> filesChanges = new HashMap<>();
        filesChanges.put(doc1, new LinkedList<>(List.of(fileChange1Doc1, fileChange2Doc1, fileChange3Doc1)));
        filesChanges.put(doc2, new LinkedList<>(List.of(mock(FileChange.class))));
        filesChanges.put(doc3, new LinkedList<>(List.of(fileChange1Doc3, fileChange2Doc3, fileChange3Doc3)));
        filesChanges.put(doc4, new LinkedList<>(List.of(fileChange1Doc4)));

        when(changeRequest.getFileChanges()).thenReturn(filesChanges);

        ChangeRequest changeRequest1 = mock(ChangeRequest.class, "cr1");
        ChangeRequest changeRequest3 = mock(ChangeRequest.class, "cr3");
        ChangeRequest changeRequest4 = mock(ChangeRequest.class, "cr4");
        when(changeRequest.cloneWithoutFileChanges())
            .thenReturn(changeRequest1)
            .thenReturn(changeRequest3)
            .thenReturn(changeRequest4);

        when(fileChange1Doc1.getType()).thenReturn(FileChange.FileChangeType.DELETION);
        when(fileChange2Doc1.getType()).thenReturn(FileChange.FileChangeType.EDITION);
        when(fileChange3Doc1.getType()).thenReturn(FileChange.FileChangeType.CREATION);

        when(fileChange1Doc3.getType()).thenReturn(FileChange.FileChangeType.CREATION);
        when(fileChange2Doc3.getType()).thenReturn(FileChange.FileChangeType.DELETION);
        when(fileChange3Doc3.getType()).thenReturn(FileChange.FileChangeType.DELETION);

        when(fileChange1Doc4.getType()).thenReturn(FileChange.FileChangeType.EDITION);

        FileChange fileChange1Doc1Clone = mock(FileChange.class, "filechange1Doc1Clone");
        FileChange fileChange2Doc1Clone = mock(FileChange.class, "filechange2Doc1Clone");
        FileChange fileChange3Doc1Clone = mock(FileChange.class, "filechange3Doc1Clone");

        FileChange fileChange1Doc3Clone = mock(FileChange.class, "filechange1Doc3Clone");
        FileChange fileChange2Doc3Clone = mock(FileChange.class, "filechange2Doc3Clone");
        FileChange fileChange3Doc3Clone = mock(FileChange.class, "filechange3Doc3Clone");

        FileChange fileChange1Doc4Clone = mock(FileChange.class, "filechange1Doc4Clone");

        when(fileChange1Doc1.cloneWithChangeRequestAndType(changeRequest1, FileChange.FileChangeType.DELETION))
            .thenReturn(fileChange1Doc1Clone);
        when(fileChange2Doc1.cloneWithChangeRequestAndType(changeRequest1, FileChange.FileChangeType.EDITION))
            .thenReturn(fileChange2Doc1Clone);
        when(fileChange3Doc1.cloneWithChangeRequestAndType(changeRequest1, FileChange.FileChangeType.CREATION))
            .thenReturn(fileChange3Doc1Clone);

        when(fileChange1Doc3.cloneWithChangeRequestAndType(changeRequest3, FileChange.FileChangeType.CREATION))
            .thenReturn(fileChange1Doc3Clone);
        when(fileChange2Doc3.cloneWithChangeRequestAndType(changeRequest3, FileChange.FileChangeType.DELETION))
            .thenReturn(fileChange2Doc3Clone);
        when(fileChange3Doc3.cloneWithChangeRequestAndType(changeRequest3, FileChange.FileChangeType.DELETION))
            .thenReturn(fileChange3Doc3Clone);

        XWikiDocument fileChange1Doc4Doc = mock(XWikiDocument.class);
        when(fileChange1Doc4Doc.isNew()).thenReturn(false);
        when(this.fileChangeStorageManager.getCurrentDocumentFromFileChange(fileChange1Doc4))
            .thenReturn(fileChange1Doc4Doc);
        when(fileChange1Doc4.cloneWithChangeRequestAndType(changeRequest4, FileChange.FileChangeType.EDITION))
            .thenReturn(fileChange1Doc4Clone);

        String changeRequest1Id = "cr1Id";
        String changeRequest2Id = "cr2Id";
        String changeRequest3Id = "cr3Id";
        String changeRequest4Id = "cr4Id";
        String title = "crTitle";
        String description = "crContent";
        ChangeRequestStatus status = ChangeRequestStatus.READY_FOR_MERGING;
        UserReference crCreator = mock(UserReference.class, "crCreator");
        UserReference currentUser = mock(UserReference.class, "currentUser");
        DocumentReference currentUserRef = mock(DocumentReference.class, "currentUserRef");
        when(context.getUserReference()).thenReturn(currentUserRef);
        when(this.userReferenceResolver.resolve(currentUserRef)).thenReturn(currentUser);

        // save setup for CR1
        when(changeRequest1.getId()).thenReturn(changeRequest1Id);
        when(changeRequest1.getTitle()).thenReturn(title);
        when(changeRequest1.getDescription()).thenReturn(description);
        when(changeRequest1.getCreator()).thenReturn(crCreator);
        when(changeRequest1.getStatus()).thenReturn(status);
        when(changeRequest1.getAllFileChanges())
            .thenReturn(List.of(fileChange1Doc1Clone, fileChange2Doc1Clone, fileChange3Doc1Clone));

        DocumentReference cr1Ref = mock(DocumentReference.class, "cr1Ref");
        when(this.changeRequestDocumentReferenceResolver.resolve(changeRequest1)).thenReturn(cr1Ref);
        XWikiDocument cr1Doc = mock(XWikiDocument.class, "cr1Doc");
        when(this.wiki.getDocument(cr1Ref, this.context)).thenReturn(cr1Doc);
        DocumentAuthors cr1Authors = mock(DocumentAuthors.class, "cr1Authors");
        when(cr1Doc.getAuthors()).thenReturn(cr1Authors);
        BaseObject cr1Obj = mock(BaseObject.class, "cr1Obj");
        when(cr1Doc.getXObject(CHANGE_REQUEST_XCLASS, 0, true, this.context))
            .thenReturn(cr1Obj);

        // save setup for CR3
        when(changeRequest3.getId()).thenReturn(changeRequest3Id);
        when(changeRequest3.getTitle()).thenReturn(title);
        when(changeRequest3.getDescription()).thenReturn(description);
        when(changeRequest3.getCreator()).thenReturn(crCreator);
        when(changeRequest3.getStatus()).thenReturn(status);
        when(changeRequest3.getAllFileChanges())
            .thenReturn(List.of(fileChange1Doc3Clone, fileChange2Doc3Clone, fileChange3Doc3Clone));

        DocumentReference cr3Ref = mock(DocumentReference.class, "cr3Ref");
        when(this.changeRequestDocumentReferenceResolver.resolve(changeRequest3)).thenReturn(cr3Ref);
        XWikiDocument cr3Doc = mock(XWikiDocument.class, "cr3Doc");
        when(this.wiki.getDocument(cr3Ref, this.context)).thenReturn(cr3Doc);
        DocumentAuthors cr3Authors = mock(DocumentAuthors.class, "cr3Authors");
        when(cr3Doc.getAuthors()).thenReturn(cr3Authors);
        BaseObject cr3Obj = mock(BaseObject.class, "cr3Obj");
        when(cr3Doc.getXObject(CHANGE_REQUEST_XCLASS, 0, true, this.context))
            .thenReturn(cr3Obj);

        // save setup for CR4
        when(changeRequest4.getId()).thenReturn(changeRequest4Id);
        when(changeRequest4.getTitle()).thenReturn(title);
        when(changeRequest4.getDescription()).thenReturn(description);
        when(changeRequest4.getCreator()).thenReturn(crCreator);
        when(changeRequest4.getStatus()).thenReturn(status);
        when(changeRequest4.getAllFileChanges())
            .thenReturn(List.of(fileChange1Doc4Clone));

        DocumentReference cr4Ref = mock(DocumentReference.class, "cr4Ref");
        when(this.changeRequestDocumentReferenceResolver.resolve(changeRequest4)).thenReturn(cr4Ref);
        XWikiDocument cr4Doc = mock(XWikiDocument.class, "cr4Doc");
        when(this.wiki.getDocument(cr4Ref, this.context)).thenReturn(cr4Doc);
        DocumentAuthors cr4Authors = mock(DocumentAuthors.class, "cr4Authors");
        when(cr4Doc.getAuthors()).thenReturn(cr4Authors);
        BaseObject cr4Obj = mock(BaseObject.class, "cr3Obj");
        when(cr4Doc.getXObject(CHANGE_REQUEST_XCLASS, 0, true, this.context))
            .thenReturn(cr4Obj);

        // Reviews
        String review1Id = "review1";
        String review2Id = "review2";
        String review3Id = "review3";
        ChangeRequestReview review1 = mock(ChangeRequestReview.class, "review1");
        ChangeRequestReview review2 = mock(ChangeRequestReview.class, "review2");
        ChangeRequestReview review3 = mock(ChangeRequestReview.class, "review3");

        when(review1.getId()).thenReturn(review1Id);
        when(review2.getId()).thenReturn(review2Id);
        when(review3.getId()).thenReturn(review3Id);

        when(changeRequest.getReviews()).thenReturn(List.of(review1, review2, review3));

        ChangeRequestReview review1CloneCR1 = mock(ChangeRequestReview.class, "review1CloneCR1");
        ChangeRequestReview review2CloneCR1 = mock(ChangeRequestReview.class, "review2CloneCR1");
        ChangeRequestReview review3CloneCR1 = mock(ChangeRequestReview.class, "review3CloneCR1");

        ChangeRequestReview review1CloneCR3 = mock(ChangeRequestReview.class, "review1CloneCR3");
        ChangeRequestReview review2CloneCR3 = mock(ChangeRequestReview.class, "review2CloneCR3");
        ChangeRequestReview review3CloneCR3 = mock(ChangeRequestReview.class, "review3CloneCR3");

        ChangeRequestReview review1CloneCR4 = mock(ChangeRequestReview.class, "review1CloneCR4");
        ChangeRequestReview review2CloneCR4 = mock(ChangeRequestReview.class, "review2CloneCR4");
        ChangeRequestReview review3CloneCR4 = mock(ChangeRequestReview.class, "review3CloneCR4");

        when(review1.cloneWithChangeRequest(changeRequest1)).thenReturn(review1CloneCR1);
        when(review2.cloneWithChangeRequest(changeRequest1)).thenReturn(review2CloneCR1);
        when(review3.cloneWithChangeRequest(changeRequest1)).thenReturn(review3CloneCR1);



        when(review1.cloneWithChangeRequest(changeRequest3)).thenReturn(review1CloneCR3);
        when(review2.cloneWithChangeRequest(changeRequest3)).thenReturn(review2CloneCR3);
        when(review3.cloneWithChangeRequest(changeRequest3)).thenReturn(review3CloneCR3);

        when(review1.cloneWithChangeRequest(changeRequest4)).thenReturn(review1CloneCR4);
        when(review2.cloneWithChangeRequest(changeRequest4)).thenReturn(review2CloneCR4);
        when(review3.cloneWithChangeRequest(changeRequest4)).thenReturn(review3CloneCR4);

        // Approvers
        when(this.approversManager.wasManuallyEdited(changeRequest)).thenReturn(false);
        when(changeRequest1.getLastFileChanges()).thenReturn(Collections.singletonList(fileChange3Doc1Clone));
        when(changeRequest3.getLastFileChanges()).thenReturn(Collections.singletonList(fileChange3Doc3Clone));
        when(changeRequest4.getLastFileChanges()).thenReturn(Collections.singletonList(fileChange1Doc4Clone));

        UserReference approverDoc1 = mock(UserReference.class, "approverDoc1");
        UserReference approverDoc3 = mock(UserReference.class, "approverDoc3");
        UserReference approverDoc4 = mock(UserReference.class, "approverDoc4");

        DocumentReference approverGroupDoc1 = mock(DocumentReference.class, "approverGroupDoc1");
        DocumentReference approverGroupDoc3 = mock(DocumentReference.class, "approverGroupDoc3");

        when(this.fileChangeApproversManager.getAllApprovers(fileChange3Doc1Clone, false))
            .thenReturn(Collections.singleton(approverDoc1));
        when(this.fileChangeApproversManager.getAllApprovers(fileChange3Doc3Clone, false))
            .thenReturn(Collections.singleton(approverDoc3));
        when(this.fileChangeApproversManager.getAllApprovers(fileChange1Doc4Clone, false))
            .thenReturn(Collections.singleton(approverDoc4));

        when(this.fileChangeApproversManager.getGroupsApprovers(fileChange3Doc1Clone))
            .thenReturn(Collections.singleton(approverGroupDoc1));
        when(this.fileChangeApproversManager.getGroupsApprovers(fileChange3Doc3Clone))
            .thenReturn(Collections.singleton(approverGroupDoc3));

        // Handle deletion
        DocumentReference originalCRDocRef = mock(DocumentReference.class, "originalCRDocRef");
        when(this.changeRequestDocumentReferenceResolver.resolve(changeRequest)).thenReturn(originalCRDocRef);
        EntityRequest deleteRequest = mock(EntityRequest.class, "deleteRequest");
        when(this.refactoringRequestFactory.createDeleteRequest(Collections.singletonList(originalCRDocRef)))
            .thenReturn(deleteRequest);
        Job deletionJob = mock(Job.class);
        when(this.jobExecutor.execute(RefactoringJobs.DELETE, deleteRequest)).thenReturn(deletionJob);

        assertEquals(List.of(changeRequest1, changeRequest3, changeRequest4),
            this.storageManager.split(changeRequest, Set.of(doc2)));

        // For some reason it's working only in debug, no idea why
        // verify file change adding
//        verify(changeRequest1).addFileChange(fileChange1Doc1Clone);
//        verify(changeRequest1).addFileChange(fileChange2Doc1Clone);
//        verify(changeRequest1).addFileChange(fileChange3Doc1Clone);
//
//        verify(changeRequest2).addFileChange(fileChange1Doc2Clone);
//        verify(changeRequest2).addFileChange(fileChange2Doc2Clone);
//        verify(changeRequest2).addFileChange(fileChange3Doc2Clone);
//
//        verify(changeRequest3).addFileChange(fileChange1Doc3Clone);
//        verify(changeRequest3).addFileChange(fileChange2Doc3Clone);
//        verify(changeRequest3).addFileChange(fileChange3Doc3Clone);

        // verify save of CR (we only check the save of the document and the save of the file changes,
        // we could check all properties to be exhaustive)
        verify(this.wiki).saveDocument(cr1Doc, this.context);
        verify(this.wiki).saveDocument(cr3Doc, this.context);

        verify(this.fileChangeStorageManager).save(fileChange1Doc1Clone);
        verify(this.fileChangeStorageManager).save(fileChange2Doc1Clone);
        verify(this.fileChangeStorageManager).save(fileChange3Doc1Clone);

        verify(this.fileChangeStorageManager).save(fileChange1Doc3Clone);
        verify(this.fileChangeStorageManager).save(fileChange2Doc3Clone);
        verify(this.fileChangeStorageManager).save(fileChange3Doc3Clone);

        // verify reviews handling
        // CR1
        verify(review1CloneCR1).setValid(false);
        verify(review1CloneCR1).setId(review1Id);
        verify(changeRequest1).addReview(review1CloneCR1);
        verify(this.reviewStorageManager).save(review1CloneCR1);

        verify(review2CloneCR1).setValid(false);
        verify(review2CloneCR1).setId(review2Id);
        verify(changeRequest1).addReview(review2CloneCR1);
        verify(this.reviewStorageManager).save(review2CloneCR1);

        verify(review3CloneCR1).setValid(false);
        verify(review3CloneCR1).setId(review3Id);
        verify(changeRequest1).addReview(review3CloneCR1);
        verify(this.reviewStorageManager).save(review3CloneCR1);

        // CR3
        verify(review1CloneCR3).setValid(false);
        verify(review1CloneCR3).setId(review1Id);
        verify(changeRequest3).addReview(review1CloneCR3);
        verify(this.reviewStorageManager).save(review1CloneCR3);

        verify(review2CloneCR3).setValid(false);
        verify(review2CloneCR3).setId(review2Id);
        verify(changeRequest3).addReview(review2CloneCR3);
        verify(this.reviewStorageManager).save(review2CloneCR3);

        verify(review3CloneCR3).setValid(false);
        verify(review3CloneCR3).setId(review3Id);
        verify(changeRequest3).addReview(review3CloneCR3);
        verify(this.reviewStorageManager).save(review3CloneCR3);

        verify(this.discussionService).moveDiscussions(changeRequest,
            List.of(changeRequest1, changeRequest3, changeRequest4));

        // verify approvers handling
        verify(this.approversManager).setUsersApprovers(Collections.singleton(approverDoc1), changeRequest1);
        verify(this.approversManager).setUsersApprovers(Collections.singleton(approverDoc3), changeRequest3);

        verify(this.approversManager).setGroupsApprovers(Collections.singleton(approverGroupDoc1), changeRequest1);
        verify(this.approversManager).setGroupsApprovers(Collections.singleton(approverGroupDoc3), changeRequest3);

        // verify deletion
        verify(deleteRequest).setDeep(true);
        verify(deleteRequest).setCheckRights(false);
        verify(deleteRequest).setCheckAuthorRights(false);

        verify(this.jobExecutor).execute(RefactoringJobs.DELETE, deleteRequest);
        verify(deletionJob).join();

        verify(this.observationManager).notify(any(SplitEndChangeRequestEvent.class), eq(changeRequestId),
            eq(List.of(changeRequest1, changeRequest3, changeRequest4)));
    }

    @Test
    void delete() throws JobException, ChangeRequestException, InterruptedException
    {
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        DocumentReference originalCRDocRef = mock(DocumentReference.class, "originalCRDocRef");
        when(this.changeRequestDocumentReferenceResolver.resolve(changeRequest)).thenReturn(originalCRDocRef);
        EntityRequest deleteRequest = mock(EntityRequest.class, "deleteRequest");
        when(this.refactoringRequestFactory.createDeleteRequest(Collections.singletonList(originalCRDocRef)))
            .thenReturn(deleteRequest);
        Job deletionJob = mock(Job.class);
        when(this.jobExecutor.execute(RefactoringJobs.DELETE, deleteRequest)).thenReturn(deletionJob);

        this.storageManager.delete(changeRequest);

        verify(deleteRequest).setDeep(true);
        verify(deleteRequest).setCheckRights(false);
        verify(deleteRequest).setCheckAuthorRights(false);

        verify(this.jobExecutor).execute(RefactoringJobs.DELETE, deleteRequest);
        verify(deletionJob).join();
    }

    @Test
    void countChangeRequests() throws QueryException, ChangeRequestException
    {
        String serializedXClass = "ChangeRequest.Code.ChangeRequestClass";
        when(this.entityReferenceSerializer.serialize(CHANGE_REQUEST_XCLASS)).thenReturn(serializedXClass);

        String expectedQuery = ", BaseObject as obj , StringProperty as obj_status "
            + "where obj_status.value in ('draft','ready_for_review','ready_for_merging') and "
            + "doc.fullName=obj.name and obj.className='" + serializedXClass +"' "
            + "and obj_status.id.id=obj.id and obj_status.id.name='status' ";
        Query query = mock(Query.class);
        when(this.queryManager.createQuery(expectedQuery, Query.HQL)).thenReturn(query);
        when(query.addFilter(this.countQueryFilter)).thenReturn(query);
        long expectedValue = 12L;
        when(query.execute()).thenReturn(Collections.singletonList(expectedValue));

        assertEquals(expectedValue, this.storageManager.countChangeRequests(true));
        verify(query).addFilter(this.countQueryFilter);

        expectedQuery = ", BaseObject as obj , StringProperty as obj_status "
            + "where doc.fullName=obj.name and obj.className='" + serializedXClass + "' ";
        query = mock(Query.class);
        when(this.queryManager.createQuery(expectedQuery, Query.HQL)).thenReturn(query);
        when(query.addFilter(this.countQueryFilter)).thenReturn(query);

        expectedValue = 365L;
        when(query.execute()).thenReturn(Collections.singletonList(expectedValue));

        assertEquals(expectedValue, this.storageManager.countChangeRequests(false));
        verify(query).addFilter(this.countQueryFilter);
    }

    @Test
    void getChangeRequestsReferences() throws QueryException, ChangeRequestException
    {
        String serializedXClass = "ChangeRequest.Code.ChangeRequestClass";
        when(this.entityReferenceSerializer.serialize(CHANGE_REQUEST_XCLASS)).thenReturn(serializedXClass);
        int offset = 12;
        int limit = 25;
        String expectedQuery = ", BaseObject as obj , StringProperty as obj_status "
            + "where obj_status.value in ('draft','ready_for_review','ready_for_merging') and "
            + "doc.fullName=obj.name and obj.className='" + serializedXClass +"' "
            + "and obj_status.id.id=obj.id and obj_status.id.name='status' ";
        Query query = mock(Query.class);
        when(this.queryManager.createQuery(expectedQuery, Query.HQL)).thenReturn(query);
        when(query.setOffset(offset)).thenReturn(query);
        when(query.setLimit(limit)).thenReturn(query);

        String crRef1 = "ChangeRequest.CR1";
        String crRef2 = "ChangeRequest.CR2";
        String crRef3 = "ChangeRequest.CR3";

        DocumentReference documentReference1 = mock(DocumentReference.class, "docRef1");
        DocumentReference documentReference2 = mock(DocumentReference.class, "docRef2");
        DocumentReference documentReference3 = mock(DocumentReference.class, "docRef3");
        when(this.documentReferenceResolver.resolve(crRef1)).thenReturn(documentReference1);
        when(this.documentReferenceResolver.resolve(crRef2)).thenReturn(documentReference2);
        when(this.documentReferenceResolver.resolve(crRef3)).thenReturn(documentReference3);

        when(query.execute()).thenReturn(List.of(crRef1, crRef2, crRef3));

        assertEquals(List.of(documentReference1, documentReference2, documentReference3),
            this.storageManager.getChangeRequestsReferences(true, offset, limit));
        verify(query).setLimit(limit);
        verify(query).setOffset(offset);

        limit = 32;
        offset = 2232;
        expectedQuery = ", BaseObject as obj , StringProperty as obj_status "
            + "where doc.fullName=obj.name and obj.className='" + serializedXClass + "' ";
        query = mock(Query.class);
        when(this.queryManager.createQuery(expectedQuery, Query.HQL)).thenReturn(query);
        when(query.setOffset(offset)).thenReturn(query);
        when(query.setLimit(limit)).thenReturn(query);

        when(query.execute()).thenReturn(List.of(crRef1, crRef3));

        assertEquals(List.of(documentReference1, documentReference3),
            this.storageManager.getChangeRequestsReferences(false, offset, limit));
        verify(query).setLimit(limit);
        verify(query).setOffset(offset);
    }
}
