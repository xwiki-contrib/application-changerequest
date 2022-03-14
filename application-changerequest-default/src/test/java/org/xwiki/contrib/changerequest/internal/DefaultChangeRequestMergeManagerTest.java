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
import java.util.Optional;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestMergeDocumentResult;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.storage.FileChangeStorageManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.store.merge.MergeDocumentResult;
import org.xwiki.store.merge.MergeManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.merge.MergeConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultChangeRequestMergeManager}.
 *
 * @version $Id$
 * @since 0.11
 */
@ComponentTest
class DefaultChangeRequestMergeManagerTest
{
    @InjectMockComponents
    private DefaultChangeRequestMergeManager crMergeManager;

    @MockComponent
    private FileChangeStorageManager fileChangeStorageManager;

    @MockComponent
    private MergeManager mergeManager;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    private XWikiContext context;

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
        when(this.fileChangeStorageManager.getPreviousDocumentFromFileChange(fileChange))
            .thenReturn(Optional.of(previousDoc));

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
        assertTrue(this.crMergeManager.hasConflicts(fileChange));
        verify(this.mergeManager)
            .mergeDocument(eq(previousDoc), eq(currentDoc), eq(modifiedDoc), any(MergeConfiguration.class));
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
        when(fileChange.getId()).thenReturn("fileChangeId");

        XWikiDocument previousDoc = mock(XWikiDocument.class);
        when(this.fileChangeStorageManager.getPreviousDocumentFromFileChange(fileChange))
            .thenReturn(Optional.of(previousDoc));
        when(previousDoc.getVersion()).thenReturn("1.1");
        when(previousDoc.getDate()).thenReturn(new Date(45));
        MergeDocumentResult mergeDocumentResult = new MergeDocumentResult(currentDoc, previousDoc, null);
        ChangeRequestMergeDocumentResult expectedResult =
            new ChangeRequestMergeDocumentResult(mergeDocumentResult, false, fileChange, "1.1", new Date(45))
                .setDocumentTitle("Some title");
        assertEquals(expectedResult, this.crMergeManager.getMergeDocumentResult(fileChange));

        when(currentDoc.getVersion()).thenReturn("1.1");
        when(currentDoc.isNew()).thenReturn(true);
        DocumentReference documentReference = mock(DocumentReference.class);
        when(currentDoc.getDocumentReference()).thenReturn(documentReference);
        when(documentReference.toString()).thenReturn("Some.Reference");

        expectedResult =
            new ChangeRequestMergeDocumentResult(mergeDocumentResult, false, fileChange, "1.1", new Date(45))
                .setDocumentTitle("Some.Reference");
        assertEquals(expectedResult, this.crMergeManager.getMergeDocumentResult(fileChange));

        when(currentDoc.getVersion()).thenReturn("1.2");
        when(fileChange.getPreviousPublishedVersion()).thenReturn("1.2");
        when(currentDoc.isNew()).thenReturn(false);

        expectedResult =
            new ChangeRequestMergeDocumentResult(mergeDocumentResult, false, fileChange, "1.1", new Date(45))
                .setDocumentTitle("Some title");
        assertEquals(expectedResult, this.crMergeManager.getMergeDocumentResult(fileChange));

        when(fileChange.getType()).thenReturn(FileChange.FileChangeType.EDITION);
        XWikiDocument nextDoc = mock(XWikiDocument.class);
        when(this.fileChangeStorageManager.getModifiedDocumentFromFileChange(fileChange)).thenReturn(nextDoc);

        DocumentReference userReference = mock(DocumentReference.class);
        when(this.context.getUserReference()).thenReturn(userReference);
        DocumentReference targetEntity = mock(DocumentReference.class);
        when(fileChange.getTargetEntity()).thenReturn(targetEntity);

        MergeDocumentResult mergeDocumentResult2 = mock(MergeDocumentResult.class);
        when(mergeManager.mergeDocument(eq(previousDoc), eq(nextDoc), eq(currentDoc), any()))
            .thenAnswer(invocationOnMock -> {
                MergeConfiguration mergeConfiguration = invocationOnMock.getArgument(3);
                assertEquals(userReference, mergeConfiguration.getUserReference());
                assertEquals(targetEntity, mergeConfiguration.getConcernedDocument());
                assertFalse(mergeConfiguration.isProvidedVersionsModifiables());
                when(mergeDocumentResult2.getCurrentDocument()).thenReturn(currentDoc);
                return mergeDocumentResult2;
            });

        expectedResult = new ChangeRequestMergeDocumentResult(mergeDocumentResult2, fileChange, "1.1", new Date(45))
            .setDocumentTitle("Some title");
        assertEquals(expectedResult, this.crMergeManager.getMergeDocumentResult(fileChange));
    }
}
