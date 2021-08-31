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

import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestConfiguration;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestMergeDocumentResult;
import org.xwiki.contrib.changerequest.ChangeRequestStatus;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.MergeApprovalStrategy;
import org.xwiki.contrib.changerequest.rights.ChangeRequestApproveRight;
import org.xwiki.contrib.changerequest.storage.FileChangeStorageManager;
import org.xwiki.extension.xar.script.XarExtensionScriptService;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.store.merge.MergeDocumentResult;
import org.xwiki.store.merge.MergeManager;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.user.UserReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.merge.MergeConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
    }

    @Test
    void hasConflict() throws ChangeRequestException
    {
        FileChange fileChange = mock(FileChange.class);
        when(fileChange.getType()).thenReturn(FileChange.FileChangeType.EDITION);
        DocumentModelBridge modifiedDoc = mock(DocumentModelBridge.class);
        DocumentModelBridge currentDoc = mock(DocumentModelBridge.class);
        DocumentModelBridge previousDoc = mock(DocumentModelBridge.class);

        when(this.fileChangeStorageManager.getModifiedDocumentFromFileChange(fileChange)).thenReturn(modifiedDoc);
        when(this.fileChangeStorageManager.getCurrentDocumentFromFileChange(fileChange)).thenReturn(currentDoc);
        when(this.fileChangeStorageManager.getPreviousDocumentFromFileChange(fileChange)).thenReturn(previousDoc);

        DocumentReference userDocReference = mock(DocumentReference.class);
        when(context.getUserReference()).thenReturn(userDocReference);

        DocumentReference modifiedDocReference = mock(DocumentReference.class);
        when(modifiedDoc.getDocumentReference()).thenReturn(modifiedDocReference);

        MergeDocumentResult mergeDocumentResult = mock(MergeDocumentResult.class);
        when(this.mergeManager
            .mergeDocument(eq(previousDoc), eq(currentDoc), eq(modifiedDoc), any(MergeConfiguration.class)))
            .thenAnswer(invocationOnMock -> {
            MergeConfiguration mergeConfiguration = invocationOnMock.getArgument(3);
            assertEquals(modifiedDocReference, mergeConfiguration.getConcernedDocument());
            assertEquals(userDocReference, mergeConfiguration.getUserReference());
            assertFalse(mergeConfiguration.isProvidedVersionsModifiables());
            return mergeDocumentResult;
        });

        when(mergeDocumentResult.hasConflicts()).thenReturn(true);
        assertTrue(this.manager.hasConflicts(fileChange));
        verify(this.mergeManager)
            .mergeDocument(eq(previousDoc), eq(currentDoc), eq(modifiedDoc), any(MergeConfiguration.class));
    }

    @Test
    void isAuthorizedToMerge()
    {
        UserReference userReference = mock(UserReference.class);
        ChangeRequest changeRequest = mock(ChangeRequest.class);

        DocumentReference userDocReference = mock(DocumentReference.class);
        when(this.userReferenceConverter.convert(userReference)).thenReturn(userDocReference);

        DocumentReference reference1 = mock(DocumentReference.class);
        DocumentReference reference2 = mock(DocumentReference.class);
        when(changeRequest.getModifiedDocuments()).thenReturn(new HashSet<>(Arrays.asList(reference1, reference2)));

        Right approvalRight = ChangeRequestApproveRight.getRight();
        when(this.authorizationManager.hasAccess(approvalRight, userDocReference, reference1)).thenReturn(false);
        when(this.authorizationManager.hasAccess(Right.EDIT, userDocReference, reference1)).thenReturn(false);
        when(this.authorizationManager.hasAccess(approvalRight, userDocReference, reference2)).thenReturn(false);
        when(this.authorizationManager.hasAccess(Right.EDIT, userDocReference, reference2)).thenReturn(false);

        assertFalse(this.manager.isAuthorizedToMerge(userReference, changeRequest));

        when(this.authorizationManager.hasAccess(approvalRight, userDocReference, reference1)).thenReturn(true);
        assertFalse(this.manager.isAuthorizedToMerge(userReference, changeRequest));

        when(this.authorizationManager.hasAccess(approvalRight, userDocReference, reference2)).thenReturn(true);
        assertFalse(this.manager.isAuthorizedToMerge(userReference, changeRequest));

        when(this.authorizationManager.hasAccess(Right.EDIT, userDocReference, reference1)).thenReturn(true);
        assertFalse(this.manager.isAuthorizedToMerge(userReference, changeRequest));

        when(this.authorizationManager.hasAccess(Right.EDIT, userDocReference, reference2)).thenReturn(true);
        assertTrue(this.manager.isAuthorizedToMerge(userReference, changeRequest));

        when(this.authorizationManager.hasAccess(approvalRight, userDocReference, reference1)).thenReturn(false);
        assertFalse(this.manager.isAuthorizedToMerge(userReference, changeRequest));
    }

    @Test
    void canBeMerged() throws Exception
    {
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(changeRequest.getStatus()).thenReturn(ChangeRequestStatus.MERGED);
        assertFalse(this.manager.canBeMerged(changeRequest));
        verify(this.configuration, never()).getMergeApprovalStrategy();

        when(changeRequest.getStatus()).thenReturn(ChangeRequestStatus.DRAFT);
        assertFalse(this.manager.canBeMerged(changeRequest));
        verify(this.configuration, never()).getMergeApprovalStrategy();

        when(changeRequest.getStatus()).thenReturn(ChangeRequestStatus.READY_FOR_REVIEW);
        String approvalStrategyHint = "approve";
        when(this.configuration.getMergeApprovalStrategy()).thenReturn(approvalStrategyHint);
        MergeApprovalStrategy strategy =
            this.componentManager.registerMockComponent(MergeApprovalStrategy.class, approvalStrategyHint);
        when(strategy.canBeMerged(changeRequest)).thenReturn(false);
        assertFalse(this.manager.canBeMerged(changeRequest));
        verify(strategy).canBeMerged(changeRequest);
        verify(changeRequest, never()).getFileChanges();

        when(strategy.canBeMerged(changeRequest)).thenReturn(true);
        FileChange fileChangeA1 = mock(FileChange.class);
        FileChange fileChangeA2 = mock(FileChange.class);
        when(fileChangeA2.getType()).thenReturn(FileChange.FileChangeType.EDITION);
        FileChange fileChangeB1 = mock(FileChange.class);
        when(fileChangeB1.getType()).thenReturn(FileChange.FileChangeType.EDITION);

        DocumentReference refA = new DocumentReference("xwiki", "Space", "RefA");
        DocumentReference refB = new DocumentReference("xwiki", "Space", "RefB");
        Map<DocumentReference, Deque<FileChange>> fileChangeMap = new LinkedHashMap<>();
        Deque<FileChange> dequeA = new LinkedList<>();
        dequeA.add(fileChangeA1);
        dequeA.add(fileChangeA2);
        fileChangeMap.put(refA, dequeA);

        Deque<FileChange> dequeB = new LinkedList<>();
        dequeB.add(fileChangeB1);
        fileChangeMap.put(refB, dequeB);

        when(changeRequest.getFileChanges()).thenReturn(fileChangeMap);
        when(changeRequest.getLatestFileChangeFor(refA)).thenReturn(Optional.of(fileChangeA2));
        when(changeRequest.getLatestFileChangeFor(refB)).thenReturn(Optional.of(fileChangeB1));

        DocumentModelBridge documentModelBridge = mock(DocumentModelBridge.class);
        when(this.fileChangeStorageManager.getModifiedDocumentFromFileChange(any())).thenReturn(documentModelBridge);
        when(documentModelBridge.getDocumentReference()).thenReturn(mock(DocumentReference.class));

        MergeDocumentResult mergeDocumentResult = mock(MergeDocumentResult.class);
        when(this.mergeManager.mergeDocument(any(), any(), any(), any())).thenReturn(mergeDocumentResult);
        when(mergeDocumentResult.hasConflicts()).thenReturn(true);

        assertFalse(this.manager.canBeMerged(changeRequest));
        verify(mergeDocumentResult).hasConflicts();
        verify(this.fileChangeStorageManager).getModifiedDocumentFromFileChange(fileChangeA2);

        // only A2 is checked since we break at first conflict
        verify(this.fileChangeStorageManager, never()).getModifiedDocumentFromFileChange(fileChangeB1);

        // this one should never be checked.
        verify(this.fileChangeStorageManager, never()).getModifiedDocumentFromFileChange(fileChangeA1);

        when(mergeDocumentResult.hasConflicts()).thenReturn(false);
        assertTrue(this.manager.canBeMerged(changeRequest));
    }

    @Test
    void getMergeDocumentResult() throws Exception
    {
        FileChange fileChange = mock(FileChange.class);
        when(fileChange.getType()).thenReturn(FileChange.FileChangeType.DELETION);
        XWikiDocument currentDoc = mock(XWikiDocument.class);
        when(this.fileChangeStorageManager.getCurrentDocumentFromFileChange(fileChange)).thenReturn(currentDoc);
        when(currentDoc.getVersion()).thenReturn("1.2");
        when(fileChange.getPreviousPublishedVersion()).thenReturn("1.1");
        when(currentDoc.isNew()).thenReturn(false);
        when(currentDoc.getRenderedTitle(this.context)).thenReturn("Some title");
        ChangeRequestMergeDocumentResult expectedResult = new ChangeRequestMergeDocumentResult(true)
            .setDocumentTitle("Some title");
        assertEquals(expectedResult, this.manager.getMergeDocumentResult(fileChange));

        when(currentDoc.getVersion()).thenReturn("1.1");
        when(currentDoc.isNew()).thenReturn(true);
        DocumentReference documentReference = mock(DocumentReference.class);
        when(currentDoc.getDocumentReference()).thenReturn(documentReference);
        when(documentReference.toString()).thenReturn("Some.Reference");
        expectedResult = new ChangeRequestMergeDocumentResult(true)
            .setDocumentTitle("Some.Reference");
        assertEquals(expectedResult, this.manager.getMergeDocumentResult(fileChange));

        when(currentDoc.getVersion()).thenReturn("1.2");
        when(fileChange.getPreviousPublishedVersion()).thenReturn("1.2");
        when(currentDoc.isNew()).thenReturn(false);

        expectedResult = new ChangeRequestMergeDocumentResult(false)
            .setDocumentTitle("Some title");
        assertEquals(expectedResult, this.manager.getMergeDocumentResult(fileChange));

        when(fileChange.getType()).thenReturn(FileChange.FileChangeType.EDITION);
        XWikiDocument nextDoc = mock(XWikiDocument.class);
        XWikiDocument previousDoc = mock(XWikiDocument.class);
        when(this.fileChangeStorageManager.getModifiedDocumentFromFileChange(fileChange)).thenReturn(nextDoc);

        when(this.fileChangeStorageManager.getPreviousDocumentFromFileChange(fileChange)).thenReturn(previousDoc);

        DocumentReference userReference = mock(DocumentReference.class);
        when(this.context.getUserReference()).thenReturn(userReference);
        DocumentReference targetEntity = mock(DocumentReference.class);
        when(fileChange.getTargetEntity()).thenReturn(targetEntity);

        MergeDocumentResult mergeDocumentResult = mock(MergeDocumentResult.class);
        when(mergeManager.mergeDocument(eq(previousDoc), eq(nextDoc), eq(currentDoc), any()))
            .thenAnswer(invocationOnMock -> {
            MergeConfiguration mergeConfiguration = invocationOnMock.getArgument(3);
            assertEquals(userReference, mergeConfiguration.getUserReference());
            assertEquals(targetEntity, mergeConfiguration.getConcernedDocument());
            assertFalse(mergeConfiguration.isProvidedVersionsModifiables());
            when(mergeDocumentResult.getCurrentDocument()).thenReturn(currentDoc);
            return mergeDocumentResult;
        });

        expectedResult = new ChangeRequestMergeDocumentResult(mergeDocumentResult)
            .setDocumentTitle("Some title");
        assertEquals(expectedResult, this.manager.getMergeDocumentResult(fileChange));
    }
}
