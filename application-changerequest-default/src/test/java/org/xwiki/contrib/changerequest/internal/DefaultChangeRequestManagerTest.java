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
package org.xwiki.contrib.changerequest.internal;

import java.util.Date;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.changerequest.ApproversManager;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestConfiguration;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestMergeManager;
import org.xwiki.contrib.changerequest.ChangeRequestReview;
import org.xwiki.contrib.changerequest.ChangeRequestStatus;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.MergeApprovalStrategy;
import org.xwiki.contrib.changerequest.events.ChangeRequestStatusChangedEvent;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.contrib.changerequest.storage.FileChangeStorageManager;
import org.xwiki.contrib.changerequest.storage.ReviewStorageManager;
import org.xwiki.extension.xar.script.XarExtensionScriptService;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.observation.ObservationManager;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.store.merge.MergeDocumentResult;
import org.xwiki.store.merge.MergeManager;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.user.UserReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultChangeRequestManager}.
 *
 * @version $Id$
 * @since 0.1
 */
@ComponentTest
class DefaultChangeRequestManagerTest
{
    @InjectMockComponents
    private DefaultChangeRequestManager manager;

    @MockComponent
    private FileChangeStorageManager fileChangeStorageManager;

    @MockComponent
    private MergeManager mergeManager;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private AuthorizationManager authorizationManager;

    @MockComponent
    private UserReferenceConverter userReferenceConverter;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @MockComponent
    private ChangeRequestConfiguration configuration;

    @MockComponent
    private ApproversManager<ChangeRequest> changeRequestApproversManager;

    @MockComponent
    private ChangeRequestStorageManager changeRequestStorageManager;

    @MockComponent
    private ReviewStorageManager reviewStorageManager;

    @MockComponent
    private ObservationManager observationManager;

    @MockComponent
    private DocumentReferenceResolver<ChangeRequest> changeRequestDocumentReferenceResolver;

    @MockComponent
    private ChangeRequestMergeManager changeRequestMergeManager;

    @MockComponent
    private ContextualLocalizationManager contextualLocalizationManager;

    private XarExtensionScriptService xarExtensionScriptService;

    private XWikiContext context;

    @BeforeComponent
    void beforeComponent() throws Exception
    {
        this.componentManager.registerComponent(ComponentManager.class, "context", this.componentManager);
        this.xarExtensionScriptService = mock(XarExtensionScriptService.class);
        this.componentManager.registerComponent(ScriptService.class, "extension.xar", this.xarExtensionScriptService);
    }

    @BeforeEach
    void setup()
    {
        this.context = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(this.context);
        when(this.contextualLocalizationManager.getTranslationPlain("changerequest.save.changestatus")).thenReturn(
            "Update status");
    }

    @Test
    void canBeMerged() throws Exception
    {
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        assertFalse(this.manager.canBeMerged(changeRequest));

        when(changeRequest.getStatus()).thenReturn(ChangeRequestStatus.READY_FOR_MERGING);
        assertTrue(this.manager.canBeMerged(changeRequest));
    }

    @Test
    void computeReadyForMergingStatus() throws Exception
    {
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(changeRequest.getStatus()).thenReturn(ChangeRequestStatus.MERGED);
        assertFalse(this.manager.canBeMerged(changeRequest));
        verify(this.configuration, never()).getMergeApprovalStrategy();

        when(changeRequest.getStatus()).thenReturn(ChangeRequestStatus.DRAFT);
        this.manager.computeReadyForMergingStatus(changeRequest);
        verifyNoInteractions(this.changeRequestStorageManager);
        verify(this.configuration, never()).getMergeApprovalStrategy();

        when(changeRequest.getStatus()).thenReturn(ChangeRequestStatus.READY_FOR_REVIEW);
        String approvalStrategyHint = "approve";
        when(this.configuration.getMergeApprovalStrategy()).thenReturn(approvalStrategyHint);
        MergeApprovalStrategy strategy =
            this.componentManager.registerMockComponent(MergeApprovalStrategy.class, approvalStrategyHint);
        when(strategy.canBeMerged(changeRequest)).thenReturn(false);
        this.manager.computeReadyForMergingStatus(changeRequest);
        verify(strategy).canBeMerged(changeRequest);
        verifyNoInteractions(this.changeRequestStorageManager);

        when(strategy.canBeMerged(changeRequest)).thenReturn(true);
        when(this.changeRequestMergeManager.hasConflict(changeRequest)).thenReturn(true);
        this.manager.computeReadyForMergingStatus(changeRequest);
        verifyNoInteractions(this.changeRequestStorageManager);
        verify(this.changeRequestMergeManager).hasConflict(changeRequest);

        when(this.changeRequestMergeManager.hasConflict(changeRequest)).thenReturn(false);
        when(changeRequest.getId()).thenReturn("someId");
        when(changeRequest.setStatus(ChangeRequestStatus.READY_FOR_MERGING)).thenReturn(changeRequest);
        this.manager.computeReadyForMergingStatus(changeRequest);
        verify(changeRequest).setStatus(ChangeRequestStatus.READY_FOR_MERGING);
        verify(this.changeRequestStorageManager).save(changeRequest, "Update status");
        verify(this.observationManager).notify(any(ChangeRequestStatusChangedEvent.class), eq("someId"),
            eq(new ChangeRequestStatus[] {
                ChangeRequestStatus.READY_FOR_REVIEW, ChangeRequestStatus.READY_FOR_MERGING }));
        verify(changeRequest).updateDate();

        when(changeRequest.getStatus()).thenReturn(ChangeRequestStatus.READY_FOR_MERGING);
        when(strategy.canBeMerged(changeRequest)).thenReturn(false);
        when(changeRequest.setStatus(ChangeRequestStatus.READY_FOR_REVIEW)).thenReturn(changeRequest);
        this.manager.computeReadyForMergingStatus(changeRequest);
        verify(changeRequest).setStatus(ChangeRequestStatus.READY_FOR_REVIEW);
        verify(this.changeRequestStorageManager, times(2)).save(changeRequest, "Update status");
        verify(this.observationManager).notify(any(ChangeRequestStatusChangedEvent.class), eq("someId"),
            eq(new ChangeRequestStatus[] {
                ChangeRequestStatus.READY_FOR_MERGING, ChangeRequestStatus.READY_FOR_REVIEW }));
        verify(changeRequest, times(2)).updateDate();
    }

    @Test
    void updateStatus() throws ChangeRequestException
    {
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(changeRequest.getStatus()).thenReturn(ChangeRequestStatus.READY_FOR_REVIEW);

        this.manager.updateStatus(changeRequest, ChangeRequestStatus.READY_FOR_REVIEW);
        verifyNoInteractions(this.changeRequestStorageManager);
        verifyNoInteractions(this.observationManager);

        when(changeRequest.getStatus()).thenReturn(ChangeRequestStatus.DRAFT);
        when(changeRequest.getId()).thenReturn("someId");
        this.manager.updateStatus(changeRequest, ChangeRequestStatus.READY_FOR_REVIEW);

        verify(changeRequest).setStatus(ChangeRequestStatus.READY_FOR_REVIEW);
        verify(this.changeRequestStorageManager).save(changeRequest, "Update status");
        verify(this.observationManager).notify(any(ChangeRequestStatusChangedEvent.class), eq("someId"),
            eq(new ChangeRequestStatus[] {ChangeRequestStatus.DRAFT, ChangeRequestStatus.READY_FOR_REVIEW}));
    }

    @Test
    void addReview() throws ChangeRequestException
    {
        UserReference userReference = mock(UserReference.class);
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        ChangeRequestReview review = new ChangeRequestReview(changeRequest, false, userReference);
        review.setNew(true);
        doAnswer(invocationOnMock -> {
            ChangeRequestReview review1 = invocationOnMock.getArgument(0);
            review.setReviewDate(review1.getReviewDate());
            return null;
        }).when(this.reviewStorageManager).save(any());
        when(changeRequest.addReview(review)).thenReturn(changeRequest);
        assertEquals(review, this.manager.addReview(changeRequest, userReference, false));
        verify(this.reviewStorageManager).save(review);
        verify(changeRequest).updateDate();
    }

    @Test
    void isFileChangeOutdated() throws ChangeRequestException
    {
        FileChange fileChange = mock(FileChange.class);
        when(fileChange.getType()).thenReturn(FileChange.FileChangeType.EDITION);
        XWikiDocument currentDoc = mock(XWikiDocument.class);
        when(this.fileChangeStorageManager.getCurrentDocumentFromFileChange(fileChange)).thenReturn(currentDoc);

        when(fileChange.getPreviousPublishedVersion()).thenReturn("1.2");
        when(currentDoc.getVersion()).thenReturn("2.1");
        assertTrue(this.manager.isFileChangeOutdated(fileChange));

        when(currentDoc.getVersion()).thenReturn("1.3");
        assertTrue(this.manager.isFileChangeOutdated(fileChange));

        when(currentDoc.getVersion()).thenReturn("1.1");
        assertTrue(this.manager.isFileChangeOutdated(fileChange));

        when(currentDoc.getVersion()).thenReturn("1.2");
        when(currentDoc.getDate()).thenReturn(new Date(42));
        when(fileChange.getPreviousPublishedVersionDate()).thenReturn(new Date(33));
        assertTrue(this.manager.isFileChangeOutdated(fileChange));

        when(fileChange.getPreviousPublishedVersionDate()).thenReturn(new Date(43));
        assertFalse(this.manager.isFileChangeOutdated(fileChange));

        when(fileChange.getPreviousPublishedVersionDate()).thenReturn(new Date(42));
        assertFalse(this.manager.isFileChangeOutdated(fileChange));

        when(fileChange.getType()).thenReturn(FileChange.FileChangeType.DELETION);
        when(currentDoc.isNew()).thenReturn(false);
        assertFalse(this.manager.isFileChangeOutdated(fileChange));

        when(currentDoc.isNew()).thenReturn(true);
        assertTrue(this.manager.isFileChangeOutdated(fileChange));

        when(fileChange.getType()).thenReturn(FileChange.FileChangeType.CREATION);
        when(currentDoc.isNew()).thenReturn(false);
        assertTrue(this.manager.isFileChangeOutdated(fileChange));

        when(currentDoc.isNew()).thenReturn(true);
        assertFalse(this.manager.isFileChangeOutdated(fileChange));
    }

    @Test
    void canDeletionBeRequested() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("xwiki", "Space", "Page");
        when(this.xarExtensionScriptService.isExtensionDocument(documentReference)).thenReturn(true);
        assertFalse(this.manager.canDeletionBeRequested(documentReference));
        verify(this.context, never()).getWiki();

        when(this.xarExtensionScriptService.isExtensionDocument(documentReference)).thenReturn(false);
        XWiki xWiki = mock(XWiki.class);
        when(this.context.getWiki()).thenReturn(xWiki);
        XWikiDocument document = mock(XWikiDocument.class);
        when(xWiki.getDocument(documentReference, context)).thenReturn(document);
        BaseClass baseClass = mock(BaseClass.class);
        when(document.getXClass()).thenReturn(baseClass);
        when(baseClass.getProperties()).thenReturn(new String[0]);
        when(document.isHidden()).thenReturn(false);

        assertTrue(this.manager.canDeletionBeRequested(documentReference));

        when(document.isHidden()).thenReturn(true);
        assertFalse(this.manager.canDeletionBeRequested(documentReference));

        when(document.isHidden()).thenReturn(false);
        when(baseClass.getProperties()).thenReturn(new String[] {"foo"});
        assertFalse(this.manager.canDeletionBeRequested(documentReference));
    }
}
