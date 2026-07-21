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
package org.xwiki.contrib.changerequest.discussions.script;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestManager;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.discussions.ChangeRequestDiscussion;
import org.xwiki.contrib.changerequest.discussions.ChangeRequestDiscussionDiffBlock;
import org.xwiki.contrib.changerequest.discussions.ChangeRequestDiscussionException;
import org.xwiki.contrib.changerequest.discussions.ChangeRequestDiscussionService;
import org.xwiki.contrib.changerequest.discussions.internal.ChangeRequestDiscussionDiffUtils;
import org.xwiki.contrib.changerequest.discussions.references.AbstractChangeRequestDiscussionContextReference;
import org.xwiki.contrib.changerequest.discussions.references.ChangeRequestCommentReference;
import org.xwiki.contrib.changerequest.discussions.references.ChangeRequestFileDiffReference;
import org.xwiki.contrib.changerequest.discussions.references.ChangeRequestLineDiffReference;
import org.xwiki.contrib.changerequest.discussions.references.ChangeRequestReference;
import org.xwiki.contrib.changerequest.discussions.references.ChangeRequestReviewReference;
import org.xwiki.contrib.changerequest.discussions.references.ChangeRequestReviewsReference;
import org.xwiki.contrib.changerequest.discussions.references.difflocation.FileDiffLocation;
import org.xwiki.contrib.changerequest.discussions.references.difflocation.LineDiffLocation;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.contrib.discussions.domain.Discussion;
import org.xwiki.contrib.discussions.domain.DiscussionContext;
import org.xwiki.contrib.discussions.domain.references.DiscussionContextReference;
import org.xwiki.contrib.discussions.domain.references.DiscussionReference;
import org.xwiki.diff.display.UnifiedDiffBlock;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.fasterxml.jackson.core.JsonProcessingException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ChangeRequestDiscussionScriptService}.
 *
 * @version $Id$
 */
@ComponentTest
class ChangeRequestDiscussionScriptServiceTest
{
    @InjectMockComponents
    private ChangeRequestDiscussionScriptService scriptService;

    @MockComponent
    private ChangeRequestDiscussionService changeRequestDiscussionService;

    @MockComponent
    @Named("withtype")
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @MockComponent
    private ChangeRequestDiscussionDiffUtils changeRequestDiscussionDiffUtils;

    @MockComponent
    private ChangeRequestManager changeRequestManager;

    @MockComponent
    private ChangeRequestStorageManager changeRequestStorageManager;

    @MockComponent
    @Named("currentmixed")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @MockComponent
    @Named("currentmixed")
    private EntityReferenceResolver<String> entityReferenceResolver;

    @Test
    void getOrCreateFileDiffReference()
    {
        ChangeRequestFileDiffReference result =
            this.scriptService.getOrCreateFileDiffReference("CR1", "xwiki:Space.Doc", "diff1");

        ChangeRequestFileDiffReference expected =
            new ChangeRequestFileDiffReference("CR1", new FileDiffLocation("diff1", "xwiki:Space.Doc"));
        assertEquals(expected, result);
    }

    @Test
    void getOrCreateDiffDiscussionOnFileDiffReferenceUsesMetadataAndUnderscore() throws Exception
    {
        FileDiffLocation fileDiffLocation = new FileDiffLocation("diff1", "xwiki:Space.Doc");
        ChangeRequestFileDiffReference fileDiffReference = new ChangeRequestFileDiffReference("CR1",
            fileDiffLocation);

        EntityReference entityReference = new EntityReference("Doc", EntityType.DOCUMENT);
        when(this.entityReferenceSerializer.serialize(entityReference))
            .thenReturn(fileDiffReference.getReference());

        LineDiffLocation expectedLineDiffLocation = new LineDiffLocation(fileDiffLocation,
            LineDiffLocation.DiffDocumentPart.METADATA, "_", "content", 5,
            LineDiffLocation.LineChange.ADDED);
        ChangeRequestLineDiffReference expectedReference =
            new ChangeRequestLineDiffReference("CR1", expectedLineDiffLocation);

        Discussion discussion = mock(Discussion.class);
        DiscussionReference discussionReference = new DiscussionReference("changerequest", "discussion1");
        when(discussion.getReference()).thenReturn(discussionReference);
        when(this.changeRequestDiscussionService.getOrCreateDiscussionFor(expectedReference))
            .thenReturn(discussion);

        assertEquals(discussionReference, this.scriptService.getOrCreateDiffDiscussion(fileDiffReference,
            entityReference, "content", 5, LineDiffLocation.LineChange.ADDED));
    }

    @Test
    void getOrCreateDiffDiscussionOnObjectUsesXObject() throws Exception
    {
        FileDiffLocation fileDiffLocation = new FileDiffLocation("diff1", "xwiki:Space.Doc");
        ChangeRequestFileDiffReference fileDiffReference = new ChangeRequestFileDiffReference("CR1",
            fileDiffLocation);

        EntityReference entityReference = new EntityReference("Some.Class[0]", EntityType.OBJECT);
        when(this.entityReferenceSerializer.serialize(entityReference)).thenReturn("xwiki:Space.Doc^Some.Class[0]");

        LineDiffLocation expectedLineDiffLocation = new LineDiffLocation(fileDiffLocation,
            LineDiffLocation.DiffDocumentPart.XOBJECT, "xwiki:Space.Doc^Some.Class[0]", "content", 5,
            LineDiffLocation.LineChange.ADDED);
        ChangeRequestLineDiffReference expectedReference =
            new ChangeRequestLineDiffReference("CR1", expectedLineDiffLocation);

        Discussion discussion = mock(Discussion.class);
        DiscussionReference discussionReference = new DiscussionReference("changerequest", "discussion1");
        when(discussion.getReference()).thenReturn(discussionReference);
        when(this.changeRequestDiscussionService.getOrCreateDiscussionFor(expectedReference))
            .thenReturn(discussion);

        assertEquals(discussionReference, this.scriptService.getOrCreateDiffDiscussion(fileDiffReference,
            entityReference, "content", 5, LineDiffLocation.LineChange.ADDED));
    }

    @Test
    void getOrCreateDiffDiscussionOnClassPropertyUsesXClass() throws Exception
    {
        FileDiffLocation fileDiffLocation = new FileDiffLocation("diff1", "xwiki:Space.Doc");
        ChangeRequestFileDiffReference fileDiffReference = new ChangeRequestFileDiffReference("CR1",
            fileDiffLocation);

        EntityReference entityReference = new EntityReference("field", EntityType.CLASS_PROPERTY);
        when(this.entityReferenceSerializer.serialize(entityReference)).thenReturn("xwiki:Space.Doc^field");

        LineDiffLocation expectedLineDiffLocation = new LineDiffLocation(fileDiffLocation,
            LineDiffLocation.DiffDocumentPart.XCLASS, "xwiki:Space.Doc^field", "content", 5,
            LineDiffLocation.LineChange.ADDED);
        ChangeRequestLineDiffReference expectedReference =
            new ChangeRequestLineDiffReference("CR1", expectedLineDiffLocation);

        Discussion discussion = mock(Discussion.class);
        DiscussionReference discussionReference = new DiscussionReference("changerequest", "discussion1");
        when(discussion.getReference()).thenReturn(discussionReference);
        when(this.changeRequestDiscussionService.getOrCreateDiscussionFor(expectedReference))
            .thenReturn(discussion);

        assertEquals(discussionReference, this.scriptService.getOrCreateDiffDiscussion(fileDiffReference,
            entityReference, "content", 5, LineDiffLocation.LineChange.ADDED));
    }

    @Test
    void getOrCreateDiffDiscussionOnAttachmentUsesAttachment() throws Exception
    {
        FileDiffLocation fileDiffLocation = new FileDiffLocation("diff1", "xwiki:Space.Doc");
        ChangeRequestFileDiffReference fileDiffReference = new ChangeRequestFileDiffReference("CR1",
            fileDiffLocation);

        EntityReference entityReference = new EntityReference("file.png", EntityType.ATTACHMENT);
        when(this.entityReferenceSerializer.serialize(entityReference)).thenReturn("xwiki:Space.Doc@file.png");

        LineDiffLocation expectedLineDiffLocation = new LineDiffLocation(fileDiffLocation,
            LineDiffLocation.DiffDocumentPart.ATTACHMENT, "xwiki:Space.Doc@file.png", "content", 5,
            LineDiffLocation.LineChange.ADDED);
        ChangeRequestLineDiffReference expectedReference =
            new ChangeRequestLineDiffReference("CR1", expectedLineDiffLocation);

        Discussion discussion = mock(Discussion.class);
        DiscussionReference discussionReference = new DiscussionReference("changerequest", "discussion1");
        when(discussion.getReference()).thenReturn(discussionReference);
        when(this.changeRequestDiscussionService.getOrCreateDiscussionFor(expectedReference))
            .thenReturn(discussion);

        assertEquals(discussionReference, this.scriptService.getOrCreateDiffDiscussion(fileDiffReference,
            entityReference, "content", 5, LineDiffLocation.LineChange.ADDED));
    }

    @Test
    void getOrCreateDiffDiscussionOnOtherTypeUsesMetadata() throws Exception
    {
        FileDiffLocation fileDiffLocation = new FileDiffLocation("diff1", "xwiki:Space.Doc");
        ChangeRequestFileDiffReference fileDiffReference = new ChangeRequestFileDiffReference("CR1",
            fileDiffLocation);

        EntityReference entityReference = new EntityReference("Other.Doc", EntityType.DOCUMENT);
        when(this.entityReferenceSerializer.serialize(entityReference)).thenReturn("xwiki:Space.OtherDoc");

        LineDiffLocation expectedLineDiffLocation = new LineDiffLocation(fileDiffLocation,
            LineDiffLocation.DiffDocumentPart.METADATA, "xwiki:Space.OtherDoc", "content", 5,
            LineDiffLocation.LineChange.ADDED);
        ChangeRequestLineDiffReference expectedReference =
            new ChangeRequestLineDiffReference("CR1", expectedLineDiffLocation);

        Discussion discussion = mock(Discussion.class);
        DiscussionReference discussionReference = new DiscussionReference("changerequest", "discussion1");
        when(discussion.getReference()).thenReturn(discussionReference);
        when(this.changeRequestDiscussionService.getOrCreateDiscussionFor(expectedReference))
            .thenReturn(discussion);

        assertEquals(discussionReference, this.scriptService.getOrCreateDiffDiscussion(fileDiffReference,
            entityReference, "content", 5, LineDiffLocation.LineChange.ADDED));
    }

    @Test
    void getOrCreateDiffDiscussionWithEmptyDiffBlockIdNormalizesToUnderscore() throws Exception
    {
        FileDiffLocation fileDiffLocation = new FileDiffLocation("diff1", "xwiki:Space.Doc");
        ChangeRequestFileDiffReference fileDiffReference = new ChangeRequestFileDiffReference("CR1",
            fileDiffLocation);

        EntityReference entityReference = new EntityReference("Other.Doc", EntityType.DOCUMENT);
        when(this.entityReferenceSerializer.serialize(entityReference)).thenReturn("xwiki:Space.OtherDoc");

        LineDiffLocation expectedLineDiffLocation = new LineDiffLocation(fileDiffLocation,
            LineDiffLocation.DiffDocumentPart.METADATA, "xwiki:Space.OtherDoc", "_", 5,
            LineDiffLocation.LineChange.ADDED);
        ChangeRequestLineDiffReference expectedReference =
            new ChangeRequestLineDiffReference("CR1", expectedLineDiffLocation);

        Discussion discussion = mock(Discussion.class);
        DiscussionReference discussionReference = new DiscussionReference("changerequest", "discussion1");
        when(discussion.getReference()).thenReturn(discussionReference);
        when(this.changeRequestDiscussionService.getOrCreateDiscussionFor(expectedReference))
            .thenReturn(discussion);

        assertEquals(discussionReference, this.scriptService.getOrCreateDiffDiscussion(fileDiffReference,
            entityReference, "", 5, LineDiffLocation.LineChange.ADDED));
    }

    @Test
    void getOrCreateChangeRequestCommentDiscussion() throws Exception
    {
        ChangeRequestCommentReference reference = new ChangeRequestCommentReference("CR1");
        Discussion discussion = mock(Discussion.class);
        DiscussionReference discussionReference = new DiscussionReference("changerequest", "discussion1");
        when(discussion.getReference()).thenReturn(discussionReference);
        when(this.changeRequestDiscussionService.getOrCreateDiscussionFor(reference)).thenReturn(discussion);

        assertEquals(discussionReference,
            this.scriptService.getOrCreateChangeRequestCommentDiscussion("CR1"));
    }

    @Test
    void getOrCreateChangeRequestReviewDiscussion() throws Exception
    {
        ChangeRequestReviewReference reference = new ChangeRequestReviewReference("review1", "CR1");
        Discussion discussion = mock(Discussion.class);
        DiscussionReference discussionReference = new DiscussionReference("changerequest", "discussion1");
        when(discussion.getReference()).thenReturn(discussionReference);
        when(this.changeRequestDiscussionService.getOrCreateDiscussionFor(reference)).thenReturn(discussion);

        assertEquals(discussionReference,
            this.scriptService.getOrCreateChangeRequestReviewDiscussion("CR1", "review1"));
    }

    @Test
    void createChangeRequestReviewsDiscussion() throws Exception
    {
        ChangeRequestReviewsReference reference = new ChangeRequestReviewsReference("CR1");
        Discussion discussion = mock(Discussion.class);
        DiscussionReference discussionReference = new DiscussionReference("changerequest", "discussion1");
        when(discussion.getReference()).thenReturn(discussionReference);
        when(this.changeRequestDiscussionService.createDiscussionFor(reference)).thenReturn(discussion);

        assertEquals(discussionReference, this.scriptService.createChangeRequestReviewsDiscussion("CR1"));
    }

    @Test
    void getOrCreateChangeRequestReviewDiscussionContext() throws Exception
    {
        ChangeRequestReviewReference reference = new ChangeRequestReviewReference("review1", "CR1");
        DiscussionContext discussionContext = mock(DiscussionContext.class);
        DiscussionContextReference discussionContextReference =
            new DiscussionContextReference("changerequest", "context1");
        when(discussionContext.getReference()).thenReturn(discussionContextReference);
        when(this.changeRequestDiscussionService.getOrCreateDiscussionContextFor(reference))
            .thenReturn(discussionContext);

        assertEquals(discussionContextReference,
            this.scriptService.getOrCreateChangeRequestReviewDiscussionContext("CR1", "review1"));
    }

    @Test
    void getDiscussionsFromChangeRequest() throws Exception
    {
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(changeRequest.getId()).thenReturn("CR1");

        Discussion discussion1 = mock(Discussion.class);
        Discussion discussion2 = mock(Discussion.class);
        List<Discussion> discussions = List.of(discussion1, discussion2);
        when(this.changeRequestDiscussionService.getDiscussionsFrom(new ChangeRequestReference("CR1")))
            .thenReturn(discussions);

        AbstractChangeRequestDiscussionContextReference reference1 =
            mock(AbstractChangeRequestDiscussionContextReference.class);
        AbstractChangeRequestDiscussionContextReference reference2 =
            mock(AbstractChangeRequestDiscussionContextReference.class);
        when(this.changeRequestDiscussionService.getReferenceFrom(discussion1)).thenReturn(reference1);
        when(this.changeRequestDiscussionService.getReferenceFrom(discussion2)).thenReturn(reference2);

        List<ChangeRequestDiscussion> result = this.scriptService.getDiscussionsFromChangeRequest(changeRequest);

        assertEquals(2, result.size());
        assertEquals(new ChangeRequestDiscussion(reference1, discussion1), result.get(0));
        assertEquals(new ChangeRequestDiscussion(reference2, discussion2), result.get(1));
    }

    @Test
    void getReference() throws Exception
    {
        Discussion discussion = mock(Discussion.class);
        AbstractChangeRequestDiscussionContextReference reference =
            mock(AbstractChangeRequestDiscussionContextReference.class);
        when(this.changeRequestDiscussionService.getReferenceFrom(discussion)).thenReturn(reference);

        assertEquals(reference, this.scriptService.getReference(discussion));
    }

    @Test
    void attachDiffBlockMetadataWithEmptyContextDiffReturnsFalse() throws Exception
    {
        DiscussionReference discussionReference = new DiscussionReference("changerequest", "discussion1");

        assertFalse(this.scriptService.attachDiffBlockMetadata(discussionReference, ""));
        verifyNoInteractions(this.changeRequestDiscussionDiffUtils);
        verifyNoInteractions(this.changeRequestDiscussionService);
    }

    @Test
    void attachDiffBlockMetadataDeserializesAndDelegates() throws Exception
    {
        DiscussionReference discussionReference = new DiscussionReference("changerequest", "discussion1");
        UnifiedDiffBlock<String, Character> diffBlock = mock(UnifiedDiffBlock.class);
        when(this.changeRequestDiscussionDiffUtils.deserialize("serializedDiff")).thenReturn(diffBlock);
        when(this.changeRequestDiscussionService.attachDiffBlockMetadata(discussionReference, diffBlock))
            .thenReturn(true);

        assertTrue(this.scriptService.attachDiffBlockMetadata(discussionReference, "serializedDiff"));
    }

    @Test
    void attachDiffBlockMetadataDeserializationErrorPropagates() throws Exception
    {
        DiscussionReference discussionReference = new DiscussionReference("changerequest", "discussion1");
        JsonProcessingException jsonException = mock(JsonProcessingException.class);
        when(this.changeRequestDiscussionDiffUtils.deserialize("serializedDiff")).thenThrow(jsonException);

        JsonProcessingException thrown = assertThrows(JsonProcessingException.class,
            () -> this.scriptService.attachDiffBlockMetadata(discussionReference, "serializedDiff"));
        assertEquals(jsonException, thrown);
        verify(this.changeRequestDiscussionService, never()).attachDiffBlockMetadata(any(), any());
    }

    @Test
    void getDiffBlockMetadataPresentReturnsBlock() throws Exception
    {
        Discussion discussion = mock(Discussion.class);
        ChangeRequestDiscussionDiffBlock diffBlock = mock(ChangeRequestDiscussionDiffBlock.class);
        when(this.changeRequestDiscussionService.getDiffBlockMetadata(discussion))
            .thenReturn(Optional.of(diffBlock));

        assertEquals(diffBlock, this.scriptService.getDiffBlockMetadata(discussion));
    }

    @Test
    void getDiffBlockMetadataAbsentReturnsNull() throws Exception
    {
        Discussion discussion = mock(Discussion.class);
        when(this.changeRequestDiscussionService.getDiffBlockMetadata(discussion)).thenReturn(Optional.empty());

        assertNull(this.scriptService.getDiffBlockMetadata(discussion));
    }

    @Test
    void getPageTitleWithUnknownChangeRequestThrows()
    {
        FileDiffLocation fileDiffLocation = new FileDiffLocation("diff1", "xwiki:Space.Doc");
        LineDiffLocation lineDiffLocation = new LineDiffLocation(fileDiffLocation,
            LineDiffLocation.DiffDocumentPart.METADATA, "_", "content", 5, LineDiffLocation.LineChange.ADDED);
        ChangeRequestLineDiffReference reference = new ChangeRequestLineDiffReference("CR1", lineDiffLocation);
        ChangeRequestDiscussionDiffBlock diffBlock =
            new ChangeRequestDiscussionDiffBlock(mock(UnifiedDiffBlock.class), reference);

        ChangeRequestException exception =
            assertThrows(ChangeRequestException.class, () -> this.scriptService.getPageTitle(diffBlock));
        assertEquals("Cannot find change request with id [CR1]", exception.getMessage());
    }

    @Test
    void getPageTitleWithUnknownFileChangeThrows() throws Exception
    {
        FileDiffLocation fileDiffLocation = new FileDiffLocation("diff1", "xwiki:Space.Doc");
        LineDiffLocation lineDiffLocation = new LineDiffLocation(fileDiffLocation,
            LineDiffLocation.DiffDocumentPart.METADATA, "_", "content", 5, LineDiffLocation.LineChange.ADDED);
        ChangeRequestLineDiffReference reference = new ChangeRequestLineDiffReference("CR1", lineDiffLocation);
        ChangeRequestDiscussionDiffBlock diffBlock =
            new ChangeRequestDiscussionDiffBlock(mock(UnifiedDiffBlock.class), reference);

        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(this.changeRequestStorageManager.load("CR1")).thenReturn(Optional.of(changeRequest));

        DocumentReference documentReference = new DocumentReference("xwiki", "Space", "Doc");
        when(this.documentReferenceResolver.resolve("xwiki:Space.Doc")).thenReturn(documentReference);
        when(changeRequest.getLatestFileChangeFor(new DocumentReference(documentReference, Locale.ROOT)))
            .thenReturn(Optional.empty());

        ChangeRequestException exception =
            assertThrows(ChangeRequestException.class, () -> this.scriptService.getPageTitle(diffBlock));
        assertEquals("Cannot find filechange with reference for [xwiki:Space.Doc()]", exception.getMessage());
    }

    @Test
    void getPageTitleWithNullLocaleUsesRootLocale() throws Exception
    {
        FileDiffLocation fileDiffLocation = new FileDiffLocation("diff1", "xwiki:Space.Doc");
        LineDiffLocation lineDiffLocation = new LineDiffLocation(fileDiffLocation,
            LineDiffLocation.DiffDocumentPart.METADATA, "_", "content", 5, LineDiffLocation.LineChange.ADDED);
        ChangeRequestLineDiffReference reference = new ChangeRequestLineDiffReference("CR1", lineDiffLocation);
        ChangeRequestDiscussionDiffBlock diffBlock =
            new ChangeRequestDiscussionDiffBlock(mock(UnifiedDiffBlock.class), reference);

        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(this.changeRequestStorageManager.load("CR1")).thenReturn(Optional.of(changeRequest));

        DocumentReference documentReference = new DocumentReference("xwiki", "Space", "Doc");
        assertNull(documentReference.getLocale());
        when(this.documentReferenceResolver.resolve("xwiki:Space.Doc")).thenReturn(documentReference);

        FileChange fileChange = mock(FileChange.class);
        when(fileChange.getId()).thenReturn("fc1");
        when(changeRequest.getLatestFileChangeFor(new DocumentReference(documentReference, Locale.ROOT)))
            .thenReturn(Optional.of(fileChange));

        when(this.changeRequestManager.getTitle("CR1", "fc1")).thenReturn("Some title");

        assertEquals("Some title", this.scriptService.getPageTitle(diffBlock));
    }

    @Test
    void getPageTitleWithExistingLocaleKeepsIt() throws Exception
    {
        FileDiffLocation fileDiffLocation = new FileDiffLocation("diff1", "xwiki:Space.Doc.fr");
        LineDiffLocation lineDiffLocation = new LineDiffLocation(fileDiffLocation,
            LineDiffLocation.DiffDocumentPart.METADATA, "_", "content", 5, LineDiffLocation.LineChange.ADDED);
        ChangeRequestLineDiffReference reference = new ChangeRequestLineDiffReference("CR1", lineDiffLocation);
        ChangeRequestDiscussionDiffBlock diffBlock =
            new ChangeRequestDiscussionDiffBlock(mock(UnifiedDiffBlock.class), reference);

        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(this.changeRequestStorageManager.load("CR1")).thenReturn(Optional.of(changeRequest));

        DocumentReference documentReference =
            new DocumentReference("xwiki", "Space", "Doc", Locale.FRENCH);
        when(this.documentReferenceResolver.resolve("xwiki:Space.Doc.fr")).thenReturn(documentReference);

        FileChange fileChange = mock(FileChange.class);
        when(fileChange.getId()).thenReturn("fc1");
        when(changeRequest.getLatestFileChangeFor(documentReference)).thenReturn(Optional.of(fileChange));

        when(this.changeRequestManager.getTitle("CR1", "fc1")).thenReturn("Titre");

        assertEquals("Titre", this.scriptService.getPageTitle(diffBlock));
    }

    @Test
    void getDiffBlockReference()
    {
        FileDiffLocation fileDiffLocation = new FileDiffLocation("diff1", "xwiki:Space.Doc");
        LineDiffLocation lineDiffLocation = new LineDiffLocation(fileDiffLocation,
            LineDiffLocation.DiffDocumentPart.METADATA, "xwiki:Space.Doc", "content", 5,
            LineDiffLocation.LineChange.ADDED);
        ChangeRequestLineDiffReference reference = new ChangeRequestLineDiffReference("CR1", lineDiffLocation);
        ChangeRequestDiscussionDiffBlock diffBlock =
            new ChangeRequestDiscussionDiffBlock(mock(UnifiedDiffBlock.class), reference);

        EntityReference expected = new EntityReference("Doc", EntityType.DOCUMENT);
        when(this.entityReferenceResolver.resolve("xwiki:Space.Doc", null)).thenReturn(expected);

        assertEquals(expected, this.scriptService.getDiffBlockReference(diffBlock));
    }
}
