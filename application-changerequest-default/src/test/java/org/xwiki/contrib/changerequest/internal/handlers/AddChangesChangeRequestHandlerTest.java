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
import java.util.Optional;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.contrib.changerequest.ApproversManager;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestManager;
import org.xwiki.contrib.changerequest.ChangeRequestMergeManager;
import org.xwiki.contrib.changerequest.ChangeRequestReference;
import org.xwiki.contrib.changerequest.ChangeRequestRightsManager;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.events.ChangeRequestFileChangeAddedEvent;
import org.xwiki.contrib.changerequest.internal.FileChangeVersionManager;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.observation.ObservationManager;
import org.xwiki.store.merge.MergeDocumentResult;
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
import com.xpn.xwiki.doc.XWikiDocumentArchive;
import com.xpn.xwiki.store.XWikiVersioningStoreInterface;
import com.xpn.xwiki.web.EditForm;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link AddChangesChangeRequestHandler}.
 *
 * @version $Id$
 * @since 0.5
 */
@ComponentTest
class AddChangesChangeRequestHandlerTest
{
    @InjectMockComponents
    private AddChangesChangeRequestHandler handler;

    @MockComponent
    private ChangeRequestManager changeRequestManager;

    @MockComponent
    private UserReferenceResolver<CurrentUserReference> userReferenceResolver;

    @MockComponent
    private FileChangeVersionManager fileChangeVersionManager;

    @MockComponent
    private ApproversManager<FileChange> fileChangeApproversManager;

    @MockComponent
    private ApproversManager<ChangeRequest> changeRequestApproversManager;

    @MockComponent
    protected Provider<XWikiContext> contextProvider;

    @MockComponent
    protected DocumentReferenceResolver<ChangeRequest> changeRequestDocumentReferenceResolver;

    @MockComponent
    protected ChangeRequestStorageManager storageManager;

    @MockComponent
    protected ObservationManager observationManager;

    @MockComponent
    @Named("current")
    protected DocumentReferenceResolver<String> documentReferenceResolver;

    @MockComponent
    private RequestParameterConverter requestParameterConverter;

    @MockComponent
    private ChangeRequestRightsManager changeRequestRightsManager;

    @MockComponent
    private ChangeRequestMergeManager changeRequestMergeManager;

    private XWikiContext context;
    private XWiki wiki;
    private XWikiVersioningStoreInterface versioningStore;

    @BeforeEach
    void setup()
    {
        this.context = mock(XWikiContext.class);
        when(contextProvider.get()).thenReturn(context);

        this.wiki = mock(XWiki.class);
        when(this.context.getWiki()).thenReturn(this.wiki);

        this.versioningStore = mock(XWikiVersioningStoreInterface.class);
        when(this.wiki.getVersioningStore()).thenReturn(this.versioningStore);
    }

    @Test
    void handleFileChangeNotExisting() throws Exception
    {

        XWikiRequest request = mock(XWikiRequest.class);
        when(context.getRequest()).thenReturn(request);
        XWikiResponse response = mock(XWikiResponse.class);
        when(context.getResponse()).thenReturn(response);
        when(this.requestParameterConverter.convert(request, response)).thenReturn(Optional.of(request));
        String docReference = "XWiki.Doc.Reference";
        when(request.getParameter("docReference")).thenReturn(docReference);
        DocumentReference documentReference = mock(DocumentReference.class);
        when(this.documentReferenceResolver.resolve(docReference)).thenReturn(documentReference);
        XWikiDocument document = mock(XWikiDocument.class);
        when(wiki.getDocument(documentReference, context)).thenReturn(document);
        when(document.clone()).thenReturn(document);
        when(document.getDocumentReferenceWithLocale()).thenReturn(documentReference);
        ChangeRequestReference changeRequestReference = mock(ChangeRequestReference.class);
        String changeRequestId = "some id";
        when(changeRequestReference.getId()).thenReturn(changeRequestId);
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(this.storageManager.load(changeRequestId)).thenReturn(Optional.of(changeRequest));
        when(changeRequest.getId()).thenReturn(changeRequestId);
        UserReference userReference = mock(UserReference.class);
        when(this.userReferenceResolver.resolve(CurrentUserReference.INSTANCE)).thenReturn(userReference);
        when(changeRequest.getLatestFileChangeFor(documentReference)).thenReturn(Optional.empty());
        when(request.getParameter(AddChangesChangeRequestHandler.PREVIOUS_VERSION_PARAMETER)).thenReturn("2.1");
        XWikiDocumentArchive documentArchive = mock(XWikiDocumentArchive.class);
        when(versioningStore.getXWikiDocumentArchive(document, context)).thenReturn(documentArchive);
        XWikiDocument previousVersionDoc = mock(XWikiDocument.class);
        when(documentArchive.loadDocument(new Version("2.1"), context)).thenReturn(previousVersionDoc);
        when(previousVersionDoc.getDate()).thenReturn(new Date(4100));
        when(this.fileChangeVersionManager.getNextFileChangeVersion("2.1", false)).thenReturn("filechange-3.1");
        FileChange expectedFileChange = new FileChange(changeRequest)
            .setAuthor(userReference)
            .setTargetEntity(documentReference)
            .setPreviousVersion("2.1")
            .setPreviousPublishedVersion("2.1", new Date(4100))
            .setVersion("filechange-3.1")
            .setModifiedDocument(document);
        when(changeRequest.addFileChange(any())).then(invocationOnMock -> {
            FileChange fileChange = invocationOnMock.getArgument(0);
            expectedFileChange.setCreationDate(fileChange.getCreationDate());
            return null;
        });
        when(this.changeRequestRightsManager.isViewAccessConsistent(changeRequest, documentReference)).thenReturn(true);
        DocumentReference changeRequestDocReference = mock(DocumentReference.class);
        when(this.changeRequestDocumentReferenceResolver.resolve(changeRequest)).thenReturn(changeRequestDocReference);
        String url = "some url";
        when(wiki.getURL(changeRequestDocReference, "view", context)).thenReturn(url);

        when(this.changeRequestRightsManager.isEditWithChangeRequestAllowed(userReference, documentReference))
            .thenReturn(true);
        this.handler.handle(changeRequestReference);
        verify(this.requestParameterConverter).convert(request, response);
        verify(document).clone();
        verify(document).readFromForm(any(EditForm.class), eq(context));
        verify(changeRequest).addFileChange(expectedFileChange);
        verify(this.storageManager).save(changeRequest);
        verify(this.changeRequestApproversManager).getAllApprovers(changeRequest, false);
        verify(this.fileChangeApproversManager).getAllApprovers(expectedFileChange, false);
        verify(this.changeRequestApproversManager).getGroupsApprovers(changeRequest);
        verify(this.fileChangeApproversManager).getGroupsApprovers(expectedFileChange);
        verify(this.observationManager)
            .notify(any(ChangeRequestFileChangeAddedEvent.class), eq(changeRequestId), eq(expectedFileChange));
        verify(response).sendRedirect(url);
    }

    @Test
    void handleFileChangeExistingNoConflict() throws Exception
    {
        XWikiRequest request = mock(XWikiRequest.class);
        when(context.getRequest()).thenReturn(request);
        XWikiResponse response = mock(XWikiResponse.class);
        when(context.getResponse()).thenReturn(response);
        when(this.requestParameterConverter.convert(request, response)).thenReturn(Optional.of(request));
        String docReference = "XWiki.Doc.Reference";
        when(request.getParameter("docReference")).thenReturn(docReference);
        DocumentReference documentReference = mock(DocumentReference.class, "editedDoc");
        when(this.documentReferenceResolver.resolve(docReference)).thenReturn(documentReference);
        XWikiDocument document = mock(XWikiDocument.class);
        when(wiki.getDocument(documentReference, context)).thenReturn(document);
        when(document.clone()).thenReturn(document);
        when(document.getDocumentReferenceWithLocale()).thenReturn(documentReference);
        ChangeRequestReference changeRequestReference = mock(ChangeRequestReference.class);
        String changeRequestId = "some id";
        when(changeRequestReference.getId()).thenReturn(changeRequestId);
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(this.storageManager.load(changeRequestId)).thenReturn(Optional.of(changeRequest));
        when(changeRequest.getId()).thenReturn(changeRequestId);
        UserReference userReference = mock(UserReference.class, "currentUser");
        when(this.userReferenceResolver.resolve(CurrentUserReference.INSTANCE)).thenReturn(userReference);
        when(request.getParameter(AddChangesChangeRequestHandler.PREVIOUS_VERSION_PARAMETER)).thenReturn("2.1");

        XWikiDocumentArchive documentArchive = mock(XWikiDocumentArchive.class);
        when(versioningStore.getXWikiDocumentArchive(document, context)).thenReturn(documentArchive);
        XWikiDocument previousVersionDoc = mock(XWikiDocument.class);
        when(documentArchive.loadDocument(new Version("2.1"), context)).thenReturn(previousVersionDoc);
        when(previousVersionDoc.getDate()).thenReturn(new Date(478));

        FileChange existingFileChange = mock(FileChange.class);
        when(changeRequest.getLatestFileChangeFor(documentReference)).thenReturn(Optional.of(existingFileChange));
        when(existingFileChange.getPreviousPublishedVersion()).thenReturn("1.1");
        when(existingFileChange.getPreviousPublishedVersionDate()).thenReturn(new Date(58));
        MergeDocumentResult mergeDocumentResult = mock(MergeDocumentResult.class);
        when(this.changeRequestMergeManager.mergeDocumentChanges(document, "2.1", changeRequest))
            .thenReturn(Optional.of(mergeDocumentResult));
        when(this.fileChangeVersionManager.getNextFileChangeVersion("2.1", true)).thenReturn("filechange-2.2");
        when(mergeDocumentResult.hasConflicts()).thenReturn(false);
        XWikiDocument mergedDocument = mock(XWikiDocument.class);
        when(mergeDocumentResult.getMergeResult()).thenReturn(mergedDocument);
        FileChange expectedFileChange = new FileChange(changeRequest)
            .setAuthor(userReference)
            .setTargetEntity(documentReference)
            .setPreviousVersion("2.1")
            .setPreviousPublishedVersion("1.1", new Date(58))
            .setVersion("filechange-2.2")
            .setModifiedDocument(mergedDocument);

        when(changeRequest.addFileChange(any())).then(invocationOnMock -> {
            FileChange fileChange = invocationOnMock.getArgument(0);
            expectedFileChange.setCreationDate(fileChange.getCreationDate());
            return null;
        });
        DocumentReference changeRequestDocReference = mock(DocumentReference.class);
        when(this.changeRequestDocumentReferenceResolver.resolve(changeRequest)).thenReturn(changeRequestDocReference);
        String url = "some url";
        when(wiki.getURL(changeRequestDocReference, "view", context)).thenReturn(url);

        when(this.changeRequestRightsManager.isEditWithChangeRequestAllowed(userReference, documentReference))
            .thenReturn(false);
        ChangeRequestException changeRequestException =
            assertThrows(ChangeRequestException.class, () -> this.handler.handle(changeRequestReference));
        assertEquals("User [currentUser] is not allowed to edit the document [editedDoc] through a change request.",
            changeRequestException.getMessage());

        when(this.changeRequestRightsManager.isEditWithChangeRequestAllowed(userReference, documentReference))
            .thenReturn(true);
        this.handler.handle(changeRequestReference);

        verify(this.requestParameterConverter, times(2)).convert(request, response);
        verify(document).clone();
        verify(document).readFromForm(any(EditForm.class), eq(context));
        verify(changeRequest).addFileChange(expectedFileChange);
        verify(this.storageManager).save(changeRequest);
        verify(this.changeRequestApproversManager).getAllApprovers(changeRequest, false);
        verify(this.fileChangeApproversManager).getAllApprovers(expectedFileChange, false);
        verify(this.changeRequestApproversManager).getGroupsApprovers(changeRequest);
        verify(this.fileChangeApproversManager).getGroupsApprovers(expectedFileChange);
        verify(this.observationManager)
            .notify(any(ChangeRequestFileChangeAddedEvent.class), eq(changeRequestId), eq(expectedFileChange));
        verify(response).sendRedirect(url);
    }
}
