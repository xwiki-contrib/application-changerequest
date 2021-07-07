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
import java.util.List;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestConfiguration;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestStatus;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.MergeApprovalStrategy;
import org.xwiki.contrib.changerequest.rights.ChangeRequestApproveRight;
import org.xwiki.contrib.changerequest.storage.FileChangeStorageManager;
import org.xwiki.model.reference.DocumentReference;
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

    private XWikiContext context;

    @BeforeComponent
    void beforeComponent() throws Exception
    {
        this.componentManager.registerComponent(ComponentManager.class, "context", this.componentManager);
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

        FileChange fileChange1 = mock(FileChange.class);
        FileChange fileChange2 = mock(FileChange.class);
        when(changeRequest.getAllFileChanges()).thenReturn(Arrays.asList(fileChange1, fileChange2));

        DocumentReference reference1 = mock(DocumentReference.class);
        DocumentReference reference2 = mock(DocumentReference.class);
        when(fileChange1.getTargetEntity()).thenReturn(reference1);
        when(fileChange2.getTargetEntity()).thenReturn(reference2);

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
        String approvalStrategyHint = "approve";
        when(this.configuration.getMergeApprovalStrategy()).thenReturn(approvalStrategyHint);
        MergeApprovalStrategy strategy =
            this.componentManager.registerMockComponent(MergeApprovalStrategy.class, approvalStrategyHint);
        when(strategy.canBeMerged(changeRequest)).thenReturn(false);

        assertFalse(this.manager.canBeMerged(changeRequest));
        verify(strategy).canBeMerged(changeRequest);
        verify(changeRequest, never()).getFileChanges();

        when(strategy.canBeMerged(changeRequest)).thenReturn(true);
        FileChange fileChange1 = mock(FileChange.class);
        FileChange fileChange2 = mock(FileChange.class);
        when(changeRequest.getAllFileChanges()).thenReturn(Arrays.asList(fileChange1, fileChange2));
        DocumentModelBridge documentModelBridge = mock(DocumentModelBridge.class);
        when(this.fileChangeStorageManager.getModifiedDocumentFromFileChange(any())).thenReturn(documentModelBridge);
        when(documentModelBridge.getDocumentReference()).thenReturn(mock(DocumentReference.class));

        MergeDocumentResult mergeDocumentResult = mock(MergeDocumentResult.class);
        when(this.mergeManager.mergeDocument(any(), any(), any(), any())).thenReturn(mergeDocumentResult);
        when(mergeDocumentResult.hasConflicts()).thenReturn(true);

        assertFalse(this.manager.canBeMerged(changeRequest));
        verify(mergeDocumentResult).hasConflicts();
        verify(this.fileChangeStorageManager).getModifiedDocumentFromFileChange(fileChange1);
        verify(this.fileChangeStorageManager, never()).getModifiedDocumentFromFileChange(fileChange2);

        when(mergeDocumentResult.hasConflicts()).thenReturn(false);
        assertTrue(this.manager.canBeMerged(changeRequest));
    }
}
