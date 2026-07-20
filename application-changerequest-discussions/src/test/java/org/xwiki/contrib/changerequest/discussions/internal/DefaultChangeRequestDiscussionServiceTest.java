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
package org.xwiki.contrib.changerequest.discussions.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.discussions.ChangeRequestDiscussionDiffBlock;
import org.xwiki.contrib.changerequest.discussions.ChangeRequestDiscussionException;
import org.xwiki.contrib.changerequest.discussions.ChangeRequestDiscussionService;
import org.xwiki.contrib.changerequest.discussions.references.AbstractChangeRequestDiscussionContextReference;
import org.xwiki.contrib.changerequest.discussions.references.ChangeRequestCommentReference;
import org.xwiki.contrib.changerequest.discussions.references.ChangeRequestFileDiffReference;
import org.xwiki.contrib.changerequest.discussions.references.ChangeRequestLineDiffReference;
import org.xwiki.contrib.changerequest.discussions.references.ChangeRequestReference;
import org.xwiki.contrib.changerequest.discussions.references.ChangeRequestReviewReference;
import org.xwiki.contrib.changerequest.discussions.references.ChangeRequestReviewsReference;
import org.xwiki.contrib.changerequest.discussions.references.difflocation.FileDiffLocation;
import org.xwiki.contrib.changerequest.discussions.references.difflocation.LineDiffLocation;
import org.xwiki.contrib.discussions.DiscussionContextService;
import org.xwiki.contrib.discussions.DiscussionException;
import org.xwiki.contrib.discussions.DiscussionService;
import org.xwiki.contrib.discussions.DiscussionStoreConfigurationParameters;
import org.xwiki.contrib.discussions.domain.Discussion;
import org.xwiki.contrib.discussions.domain.DiscussionContext;
import org.xwiki.contrib.discussions.domain.references.DiscussionContextEntityReference;
import org.xwiki.contrib.discussions.domain.references.DiscussionContextReference;
import org.xwiki.contrib.discussions.domain.references.DiscussionReference;
import org.xwiki.diff.display.UnifiedDiffBlock;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.fasterxml.jackson.core.JsonProcessingException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultChangeRequestDiscussionService}.
 *
 * @version $Id$
 * @since 0.6
 */
@ComponentTest
class DefaultChangeRequestDiscussionServiceTest
{
    @InjectMockComponents
    private DefaultChangeRequestDiscussionService changeRequestDiscussionService;

    @MockComponent
    private DiscussionContextService discussionContextService;

    @MockComponent
    private DiscussionService discussionService;

    @MockComponent
    private ChangeRequestDiscussionReferenceUtils discussionReferenceUtils;

    @MockComponent
    private ChangeRequestDiscussionFactory changeRequestDiscussionFactory;

    @MockComponent
    private ChangeRequestDiscussionDiffUtils changeRequestDiscussionDiffUtils;

    @MockComponent
    @Named("compactwiki")
    private EntityReferenceSerializer<String> stringEntityReferenceSerializer;

    @MockComponent
    @Named("withtype")
    private EntityReferenceSerializer<String> withTypeEntityReferenceSerializer;

    @MockComponent
    private EntityReferenceResolver<String> entityReferenceResolver;

    @MockComponent
    private ContextualLocalizationManager localizationManager;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @Test
    void getReferencesFrom() throws Exception
    {
        Discussion discussion = mock(Discussion.class);
        DiscussionContext crContext = mock(DiscussionContext.class);
        DiscussionContext fileDiffContext = mock(DiscussionContext.class);
        DiscussionContext lineDiffContext = mock(DiscussionContext.class);
        AbstractChangeRequestDiscussionContextReference changeRequestReference =
            mock(AbstractChangeRequestDiscussionContextReference.class);
        AbstractChangeRequestDiscussionContextReference lineDiffReference =
            mock(AbstractChangeRequestDiscussionContextReference.class);
        AbstractChangeRequestDiscussionContextReference fileDiffReference =
            mock(AbstractChangeRequestDiscussionContextReference.class);

        when(this.discussionReferenceUtils.computeReferenceFromContext(crContext, null))
            .thenReturn(changeRequestReference);
        when(this.discussionReferenceUtils.computeReferenceFromContext(lineDiffContext, changeRequestReference))
            .thenReturn(lineDiffReference);
        when(this.discussionReferenceUtils.computeReferenceFromContext(fileDiffContext, lineDiffReference))
            .thenReturn(lineDiffReference);
        when(this.discussionReferenceUtils.computeReferenceFromContext(fileDiffContext, changeRequestReference))
            .thenReturn(fileDiffReference);

        DiscussionReference discussionReference = mock(DiscussionReference.class);
        when(discussion.getReference()).thenReturn(discussionReference);
        when(discussionContextService.findByDiscussionReference(discussionReference)).thenReturn(
            Collections.singletonList(crContext));

        assertEquals(changeRequestReference, this.changeRequestDiscussionService.getReferenceFrom(discussion));

        when(discussionContextService.findByDiscussionReference(discussionReference)).thenReturn(Arrays.asList(
            crContext,
            lineDiffContext,
            fileDiffContext
        ));

        assertEquals(lineDiffReference, this.changeRequestDiscussionService.getReferenceFrom(discussion));

        when(discussionContextService.findByDiscussionReference(discussionReference)).thenReturn(Arrays.asList(
            crContext,
            fileDiffContext
        ));
        assertEquals(fileDiffReference, this.changeRequestDiscussionService.getReferenceFrom(discussion));
    }

    @Test
    void getOrCreateDiscussionForChangeRequestReference() throws Exception
    {
        ChangeRequestReference reference = new ChangeRequestReference("CR1");

        DiscussionContext crContext = mock(DiscussionContext.class);
        DiscussionContextReference crContextRef = new DiscussionContextReference("changerequest", "crContextRef");
        when(crContext.getReference()).thenReturn(crContextRef);
        when(this.changeRequestDiscussionFactory.getOrCreateContextFor(reference)).thenReturn(crContext);

        when(this.discussionReferenceUtils.getTitleTranslation(
            ChangeRequestDiscussionReferenceUtils.DISCUSSION_TRANSLATION_PREFIX, reference)).thenReturn("title");
        when(this.discussionReferenceUtils.getDescriptionTranslation(
            ChangeRequestDiscussionReferenceUtils.DISCUSSION_TRANSLATION_PREFIX, reference))
            .thenReturn("description");

        DiscussionStoreConfigurationParameters params = new DiscussionStoreConfigurationParameters();
        when(this.changeRequestDiscussionFactory.createDiscussionStoreConfigurationParametersFor(reference))
            .thenReturn(params);

        Discussion expectedDiscussion = mock(Discussion.class);
        when(this.discussionService.getOrCreate(ChangeRequestDiscussionService.APPLICATION_HINT, "title",
            "description", List.of(crContextRef), params)).thenReturn(expectedDiscussion);

        assertEquals(expectedDiscussion, this.changeRequestDiscussionService.getOrCreateDiscussionFor(reference));
    }

    @Test
    void getOrCreateDiscussionForFileDiffReference() throws Exception
    {
        FileDiffLocation fileDiffLocation = new FileDiffLocation("diff1", "xwiki:Space.Doc");
        ChangeRequestFileDiffReference reference = new ChangeRequestFileDiffReference("CR1", fileDiffLocation);
        ChangeRequestReference crReference = new ChangeRequestReference("CR1");

        DiscussionContext crContext = mock(DiscussionContext.class);
        DiscussionContext fileDiffContext = mock(DiscussionContext.class);
        DiscussionContextReference crContextRef = new DiscussionContextReference("changerequest", "crContextRef");
        DiscussionContextReference fileDiffContextRef =
            new DiscussionContextReference("changerequest", "fileDiffContextRef");
        when(crContext.getReference()).thenReturn(crContextRef);
        when(fileDiffContext.getReference()).thenReturn(fileDiffContextRef);

        when(this.changeRequestDiscussionFactory.getOrCreateContextFor(crReference)).thenReturn(crContext);
        when(this.changeRequestDiscussionFactory.getOrCreateContextFor(reference)).thenReturn(fileDiffContext);

        when(this.discussionReferenceUtils.getTitleTranslation(anyString(), eq(reference))).thenReturn("title");
        when(this.discussionReferenceUtils.getDescriptionTranslation(anyString(), eq(reference)))
            .thenReturn("description");

        DiscussionStoreConfigurationParameters params = new DiscussionStoreConfigurationParameters();
        when(this.changeRequestDiscussionFactory.createDiscussionStoreConfigurationParametersFor(reference))
            .thenReturn(params);

        Discussion expectedDiscussion = mock(Discussion.class);
        when(this.discussionService.getOrCreate(ChangeRequestDiscussionService.APPLICATION_HINT, "title",
            "description", List.of(crContextRef, fileDiffContextRef), params)).thenReturn(expectedDiscussion);

        assertEquals(expectedDiscussion, this.changeRequestDiscussionService.getOrCreateDiscussionFor(reference));
    }

    @Test
    void getOrCreateDiscussionForLineDiffReference() throws Exception
    {
        FileDiffLocation fileDiffLocation = new FileDiffLocation("diff1", "xwiki:Space.Doc");
        LineDiffLocation lineDiffLocation = new LineDiffLocation(fileDiffLocation,
            LineDiffLocation.DiffDocumentPart.METADATA, "xwiki:Space.Doc", "content", 12,
            LineDiffLocation.LineChange.ADDED);
        ChangeRequestLineDiffReference reference = new ChangeRequestLineDiffReference("CR1", lineDiffLocation);
        ChangeRequestReference crReference = new ChangeRequestReference("CR1");
        ChangeRequestFileDiffReference derivedFileDiffReference =
            new ChangeRequestFileDiffReference("CR1", fileDiffLocation);

        DiscussionContext crContext = mock(DiscussionContext.class);
        DiscussionContext lineDiffContext = mock(DiscussionContext.class);
        DiscussionContext fileDiffContext = mock(DiscussionContext.class);
        DiscussionContextReference crContextRef = new DiscussionContextReference("changerequest", "crContextRef");
        DiscussionContextReference lineDiffContextRef =
            new DiscussionContextReference("changerequest", "lineDiffContextRef");
        DiscussionContextReference fileDiffContextRef =
            new DiscussionContextReference("changerequest", "fileDiffContextRef");
        when(crContext.getReference()).thenReturn(crContextRef);
        when(lineDiffContext.getReference()).thenReturn(lineDiffContextRef);
        when(fileDiffContext.getReference()).thenReturn(fileDiffContextRef);

        when(this.changeRequestDiscussionFactory.getOrCreateContextFor(crReference)).thenReturn(crContext);
        when(this.changeRequestDiscussionFactory.getOrCreateContextFor(reference)).thenReturn(lineDiffContext);
        when(this.changeRequestDiscussionFactory.getOrCreateContextFor(derivedFileDiffReference))
            .thenReturn(fileDiffContext);

        when(this.discussionReferenceUtils.getTitleTranslation(anyString(), eq(reference))).thenReturn("title");
        when(this.discussionReferenceUtils.getDescriptionTranslation(anyString(), eq(reference)))
            .thenReturn("description");

        DiscussionStoreConfigurationParameters params = new DiscussionStoreConfigurationParameters();
        when(this.changeRequestDiscussionFactory.createDiscussionStoreConfigurationParametersFor(reference))
            .thenReturn(params);

        Discussion expectedDiscussion = mock(Discussion.class);
        when(this.discussionService.getOrCreate(ChangeRequestDiscussionService.APPLICATION_HINT, "title",
            "description", List.of(crContextRef, lineDiffContextRef, fileDiffContextRef), params))
            .thenReturn(expectedDiscussion);

        assertEquals(expectedDiscussion, this.changeRequestDiscussionService.getOrCreateDiscussionFor(reference));
    }

    @Test
    void getOrCreateDiscussionForReviewReference() throws Exception
    {
        ChangeRequestReviewReference reference = new ChangeRequestReviewReference("review1", "CR1");
        ChangeRequestReference crReference = new ChangeRequestReference("CR1");
        ChangeRequestReviewsReference reviewsReference = new ChangeRequestReviewsReference("CR1");

        DiscussionContext crContext = mock(DiscussionContext.class);
        DiscussionContext reviewContext = mock(DiscussionContext.class);
        DiscussionContext reviewsContext = mock(DiscussionContext.class);
        DiscussionContextReference crContextRef = new DiscussionContextReference("changerequest", "crContextRef");
        DiscussionContextReference reviewContextRef =
            new DiscussionContextReference("changerequest", "reviewContextRef");
        DiscussionContextReference reviewsContextRef =
            new DiscussionContextReference("changerequest", "reviewsContextRef");
        when(crContext.getReference()).thenReturn(crContextRef);
        when(reviewContext.getReference()).thenReturn(reviewContextRef);
        when(reviewsContext.getReference()).thenReturn(reviewsContextRef);

        when(this.changeRequestDiscussionFactory.getOrCreateContextFor(crReference)).thenReturn(crContext);
        when(this.changeRequestDiscussionFactory.getOrCreateContextFor(reference)).thenReturn(reviewContext);
        when(this.changeRequestDiscussionFactory.getOrCreateContextFor(reviewsReference)).thenReturn(reviewsContext);

        when(this.discussionReferenceUtils.getTitleTranslation(anyString(), eq(reference))).thenReturn("title");
        when(this.discussionReferenceUtils.getDescriptionTranslation(anyString(), eq(reference)))
            .thenReturn("description");

        DiscussionStoreConfigurationParameters params = new DiscussionStoreConfigurationParameters();
        when(this.changeRequestDiscussionFactory.createDiscussionStoreConfigurationParametersFor(reference))
            .thenReturn(params);

        Discussion expectedDiscussion = mock(Discussion.class);
        when(this.discussionService.getOrCreate(ChangeRequestDiscussionService.APPLICATION_HINT, "title",
            "description", List.of(crContextRef, reviewContextRef, reviewsContextRef), params))
            .thenReturn(expectedDiscussion);

        assertEquals(expectedDiscussion, this.changeRequestDiscussionService.getOrCreateDiscussionFor(reference));
    }

    @Test
    void getOrCreateDiscussionForWrapsDiscussionException() throws Exception
    {
        ChangeRequestReference reference = new ChangeRequestReference("CR1");
        when(this.changeRequestDiscussionFactory.getOrCreateContextFor(reference)).thenReturn(mock(
            DiscussionContext.class));
        when(this.discussionReferenceUtils.getTitleTranslation(any(), any())).thenReturn("title");
        when(this.discussionReferenceUtils.getDescriptionTranslation(any(), any())).thenReturn("description");
        when(this.changeRequestDiscussionFactory.createDiscussionStoreConfigurationParametersFor(reference))
            .thenReturn(new DiscussionStoreConfigurationParameters());

        DiscussionException discussionException = new DiscussionException("error");
        when(this.discussionService.getOrCreate(any(), any(), any(), any(), any())).thenThrow(discussionException);

        ChangeRequestDiscussionException exception = assertThrows(ChangeRequestDiscussionException.class,
            () -> this.changeRequestDiscussionService.getOrCreateDiscussionFor(reference));
        assertEquals(discussionException, exception.getCause());
    }

    @Test
    void createDiscussionForNonUniqueCreatesAndLinksContexts() throws Exception
    {
        ChangeRequestReference reference = new ChangeRequestReference("CR1");

        DiscussionContext crContext = mock(DiscussionContext.class);
        DiscussionContextReference crContextRef = new DiscussionContextReference("changerequest", "crContextRef");
        when(crContext.getReference()).thenReturn(crContextRef);
        when(this.changeRequestDiscussionFactory.getOrCreateContextFor(reference)).thenReturn(crContext);

        when(this.discussionReferenceUtils.getTitleTranslation(
            ChangeRequestDiscussionReferenceUtils.DISCUSSION_TRANSLATION_PREFIX, reference)).thenReturn("title");
        when(this.discussionReferenceUtils.getDescriptionTranslation(
            ChangeRequestDiscussionReferenceUtils.DISCUSSION_TRANSLATION_PREFIX, reference))
            .thenReturn("description");

        DiscussionStoreConfigurationParameters params = new DiscussionStoreConfigurationParameters();
        when(this.changeRequestDiscussionFactory.createDiscussionStoreConfigurationParametersFor(reference))
            .thenReturn(params);

        Discussion expectedDiscussion = mock(Discussion.class);
        when(this.discussionService.create(ChangeRequestDiscussionService.APPLICATION_HINT, "title", "description",
            null, params)).thenReturn(expectedDiscussion);

        assertEquals(expectedDiscussion, this.changeRequestDiscussionService.createDiscussionFor(reference));

        verify(this.discussionContextService).link(crContext, expectedDiscussion);
        verify(this.discussionService, never()).findByDiscussionContexts(any());
    }

    @Test
    void createDiscussionForUniqueWithExistingDiscussionThrows() throws Exception
    {
        FileDiffLocation fileDiffLocation = new FileDiffLocation("diff1", "xwiki:Space.Doc");
        LineDiffLocation lineDiffLocation = new LineDiffLocation(fileDiffLocation,
            LineDiffLocation.DiffDocumentPart.METADATA, "xwiki:Space.Doc", "content", 12,
            LineDiffLocation.LineChange.ADDED);
        ChangeRequestLineDiffReference reference = new ChangeRequestLineDiffReference("CR1", lineDiffLocation);

        when(this.changeRequestDiscussionFactory.getOrCreateContextFor(any())).thenReturn(mock(
            DiscussionContext.class));
        when(this.discussionService.findByDiscussionContexts(any())).thenReturn(List.of(mock(Discussion.class)));

        assertThrows(ChangeRequestDiscussionException.class,
            () -> this.changeRequestDiscussionService.createDiscussionFor(reference));

        verify(this.discussionService, never()).create(any(), any(), any(), any(), any());
    }

    @Test
    void createDiscussionForUniqueWithoutExistingDiscussionCreates() throws Exception
    {
        FileDiffLocation fileDiffLocation = new FileDiffLocation("diff1", "xwiki:Space.Doc");
        LineDiffLocation lineDiffLocation = new LineDiffLocation(fileDiffLocation,
            LineDiffLocation.DiffDocumentPart.METADATA, "xwiki:Space.Doc", "content", 12,
            LineDiffLocation.LineChange.ADDED);
        ChangeRequestLineDiffReference reference = new ChangeRequestLineDiffReference("CR1", lineDiffLocation);
        ChangeRequestReference crReference = new ChangeRequestReference("CR1");
        ChangeRequestFileDiffReference derivedFileDiffReference =
            new ChangeRequestFileDiffReference("CR1", fileDiffLocation);

        DiscussionContext crContext = mock(DiscussionContext.class);
        DiscussionContext lineDiffContext = mock(DiscussionContext.class);
        DiscussionContext fileDiffContext = mock(DiscussionContext.class);
        DiscussionContextReference crContextRef = new DiscussionContextReference("changerequest", "crContextRef");
        DiscussionContextReference lineDiffContextRef =
            new DiscussionContextReference("changerequest", "lineDiffContextRef");
        DiscussionContextReference fileDiffContextRef =
            new DiscussionContextReference("changerequest", "fileDiffContextRef");
        when(crContext.getReference()).thenReturn(crContextRef);
        when(lineDiffContext.getReference()).thenReturn(lineDiffContextRef);
        when(fileDiffContext.getReference()).thenReturn(fileDiffContextRef);

        when(this.changeRequestDiscussionFactory.getOrCreateContextFor(crReference)).thenReturn(crContext);
        when(this.changeRequestDiscussionFactory.getOrCreateContextFor(reference)).thenReturn(lineDiffContext);
        when(this.changeRequestDiscussionFactory.getOrCreateContextFor(derivedFileDiffReference))
            .thenReturn(fileDiffContext);

        when(this.discussionService.findByDiscussionContexts(
            List.of(crContextRef, lineDiffContextRef, fileDiffContextRef))).thenReturn(List.of());

        when(this.discussionReferenceUtils.getTitleTranslation(anyString(), eq(reference))).thenReturn("title");
        when(this.discussionReferenceUtils.getDescriptionTranslation(anyString(), eq(reference)))
            .thenReturn("description");

        DiscussionStoreConfigurationParameters params = new DiscussionStoreConfigurationParameters();
        when(this.changeRequestDiscussionFactory.createDiscussionStoreConfigurationParametersFor(reference))
            .thenReturn(params);

        Discussion expectedDiscussion = mock(Discussion.class);
        when(this.discussionService.create(ChangeRequestDiscussionService.APPLICATION_HINT, "title", "description",
            null, params)).thenReturn(expectedDiscussion);

        assertEquals(expectedDiscussion, this.changeRequestDiscussionService.createDiscussionFor(reference));

        verify(this.discussionContextService).link(crContext, expectedDiscussion);
        verify(this.discussionContextService).link(lineDiffContext, expectedDiscussion);
        verify(this.discussionContextService).link(fileDiffContext, expectedDiscussion);
    }

    @Test
    void createDiscussionForWrapsDiscussionException() throws Exception
    {
        ChangeRequestReference reference = new ChangeRequestReference("CR1");
        when(this.changeRequestDiscussionFactory.getOrCreateContextFor(reference)).thenReturn(mock(
            DiscussionContext.class));
        when(this.discussionReferenceUtils.getTitleTranslation(any(), any())).thenReturn("title");
        when(this.discussionReferenceUtils.getDescriptionTranslation(any(), any())).thenReturn("description");
        when(this.changeRequestDiscussionFactory.createDiscussionStoreConfigurationParametersFor(reference))
            .thenReturn(new DiscussionStoreConfigurationParameters());

        DiscussionException discussionException = new DiscussionException("error");
        when(this.discussionService.create(any(), any(), any(), any(), any())).thenThrow(discussionException);

        ChangeRequestDiscussionException exception = assertThrows(ChangeRequestDiscussionException.class,
            () -> this.changeRequestDiscussionService.createDiscussionFor(reference));
        assertEquals(discussionException, exception.getCause());
    }

    @Test
    void getDiscussionsFromContextExists() throws Exception
    {
        ChangeRequestReference reference = new ChangeRequestReference("CR1");
        when(this.changeRequestDiscussionFactory.isContextExistingFor(reference)).thenReturn(true);

        DiscussionContext context = mock(DiscussionContext.class);
        DiscussionContextReference contextRef = new DiscussionContextReference("changerequest", "contextRef");
        when(context.getReference()).thenReturn(contextRef);
        when(this.changeRequestDiscussionFactory.getOrCreateContextFor(reference)).thenReturn(context);

        List<Discussion> discussions = List.of(mock(Discussion.class));
        when(this.discussionService.findByDiscussionContexts(Collections.singletonList(contextRef)))
            .thenReturn(discussions);

        assertEquals(discussions, this.changeRequestDiscussionService.getDiscussionsFrom(reference));
    }

    @Test
    void getDiscussionsFromContextDoesNotExist() throws Exception
    {
        ChangeRequestReference reference = new ChangeRequestReference("CR1");
        when(this.changeRequestDiscussionFactory.isContextExistingFor(reference)).thenReturn(false);

        assertEquals(List.of(), this.changeRequestDiscussionService.getDiscussionsFrom(reference));
        verifyNoInteractions(this.discussionService);
    }

    @Test
    void getOrCreateDiscussionContextForDelegatesToFactory() throws Exception
    {
        ChangeRequestReference reference = new ChangeRequestReference("CR1");
        DiscussionContext context = mock(DiscussionContext.class);
        when(this.changeRequestDiscussionFactory.getOrCreateContextFor(reference)).thenReturn(context);

        assertEquals(context, this.changeRequestDiscussionService.getOrCreateDiscussionContextFor(reference));
    }

    @Test
    void attachDiffBlockMetadataDiscussionNotFoundReturnsFalse() throws Exception
    {
        DiscussionReference discussionReference = new DiscussionReference("changerequest", "discussion1");
        when(this.discussionService.get(discussionReference)).thenReturn(Optional.empty());
        UnifiedDiffBlock<String, Character> block = mock(UnifiedDiffBlock.class);

        assertFalse(this.changeRequestDiscussionService.attachDiffBlockMetadata(discussionReference, block));
        verifyNoInteractions(this.discussionContextService);
    }

    @Test
    void attachDiffBlockMetadataNotLineDiffReturnsFalse() throws Exception
    {
        DiscussionReference discussionReference = new DiscussionReference("changerequest", "discussion1");
        Discussion discussion = mock(Discussion.class);
        when(discussion.getReference()).thenReturn(discussionReference);
        when(this.discussionService.get(discussionReference)).thenReturn(Optional.of(discussion));

        DiscussionContext crContext = mock(DiscussionContext.class);
        when(this.discussionContextService.findByDiscussionReference(discussionReference))
            .thenReturn(List.of(crContext));
        ChangeRequestReference crReference = new ChangeRequestReference("CR1");
        when(this.discussionReferenceUtils.computeReferenceFromContext(crContext, null)).thenReturn(crReference);

        UnifiedDiffBlock<String, Character> block = mock(UnifiedDiffBlock.class);
        assertFalse(this.changeRequestDiscussionService.attachDiffBlockMetadata(discussionReference, block));
        verify(this.discussionContextService, never()).saveMetadata(any(), any());
        assertEquals(1, this.logCapture.size());
        assertEquals("Trying to attach a diff context to a reference not of type line diff: "
            + "[changeRequestId = [CR1], type = [CHANGE_REQUEST], reference = []]", this.logCapture.getMessage(0));
    }

    @Test
    void attachDiffBlockMetadataLineDiffSavesMetadataAndReturnsTrue() throws Exception
    {
        DiscussionReference discussionReference = new DiscussionReference("changerequest", "discussion1");
        Discussion discussion = mock(Discussion.class);
        when(discussion.getReference()).thenReturn(discussionReference);
        when(this.discussionService.get(discussionReference)).thenReturn(Optional.of(discussion));

        DiscussionContext lineDiffDiscussionContext = mock(DiscussionContext.class);
        when(this.discussionContextService.findByDiscussionReference(discussionReference))
            .thenReturn(List.of(lineDiffDiscussionContext));

        FileDiffLocation fileDiffLocation = new FileDiffLocation("diff1", "xwiki:Space.Doc");
        LineDiffLocation lineDiffLocation = new LineDiffLocation(fileDiffLocation,
            LineDiffLocation.DiffDocumentPart.METADATA, "xwiki:Space.Doc", "content", 42,
            LineDiffLocation.LineChange.ADDED);
        ChangeRequestLineDiffReference lineDiffReference = new ChangeRequestLineDiffReference("CR1",
            lineDiffLocation);
        when(this.discussionReferenceUtils.computeReferenceFromContext(lineDiffDiscussionContext, null))
            .thenReturn(lineDiffReference);

        DiscussionContext contextForReference = mock(DiscussionContext.class);
        when(this.changeRequestDiscussionFactory.getOrCreateContextFor(lineDiffReference))
            .thenReturn(contextForReference);

        UnifiedDiffBlock<String, Character> block = mock(UnifiedDiffBlock.class);
        when(this.changeRequestDiscussionDiffUtils.serialize(block)).thenReturn("{\"serialized\":true}");

        assertTrue(this.changeRequestDiscussionService.attachDiffBlockMetadata(discussionReference, block));

        verify(this.discussionContextService).saveMetadata(contextForReference,
            Collections.singletonMap(ChangeRequestDiscussionService.DIFF_CONTEXT_METADATA_KEY,
                "{\"serialized\":true}"));
    }

    @Test
    void attachDiffBlockMetadataSerializationErrorWrapsException() throws Exception
    {
        DiscussionReference discussionReference = new DiscussionReference("changerequest", "discussion1");
        Discussion discussion = mock(Discussion.class);
        when(discussion.getReference()).thenReturn(discussionReference);
        when(this.discussionService.get(discussionReference)).thenReturn(Optional.of(discussion));

        DiscussionContext lineDiffDiscussionContext = mock(DiscussionContext.class);
        when(this.discussionContextService.findByDiscussionReference(discussionReference))
            .thenReturn(List.of(lineDiffDiscussionContext));

        FileDiffLocation fileDiffLocation = new FileDiffLocation("diff1", "xwiki:Space.Doc");
        LineDiffLocation lineDiffLocation = new LineDiffLocation(fileDiffLocation,
            LineDiffLocation.DiffDocumentPart.METADATA, "xwiki:Space.Doc", "content", 42,
            LineDiffLocation.LineChange.ADDED);
        ChangeRequestLineDiffReference lineDiffReference = new ChangeRequestLineDiffReference("CR1",
            lineDiffLocation);
        when(this.discussionReferenceUtils.computeReferenceFromContext(lineDiffDiscussionContext, null))
            .thenReturn(lineDiffReference);
        when(this.changeRequestDiscussionFactory.getOrCreateContextFor(lineDiffReference))
            .thenReturn(mock(DiscussionContext.class));

        UnifiedDiffBlock<String, Character> block = mock(UnifiedDiffBlock.class);
        JsonProcessingException jsonException = mock(JsonProcessingException.class);
        when(this.changeRequestDiscussionDiffUtils.serialize(block)).thenThrow(jsonException);

        ChangeRequestDiscussionException exception = assertThrows(ChangeRequestDiscussionException.class,
            () -> this.changeRequestDiscussionService.attachDiffBlockMetadata(discussionReference, block));
        assertEquals(jsonException, exception.getCause());
    }

    @Test
    void getDiffBlockMetadataNotLineDiffReturnsEmpty() throws Exception
    {
        Discussion discussion = mock(Discussion.class);
        DiscussionReference discussionReference = new DiscussionReference("changerequest", "discussion1");
        when(discussion.getReference()).thenReturn(discussionReference);
        DiscussionContext crContext = mock(DiscussionContext.class);
        when(this.discussionContextService.findByDiscussionReference(discussionReference))
            .thenReturn(List.of(crContext));
        ChangeRequestReference crReference = new ChangeRequestReference("CR1");
        when(this.discussionReferenceUtils.computeReferenceFromContext(crContext, null)).thenReturn(crReference);

        assertEquals(Optional.empty(), this.changeRequestDiscussionService.getDiffBlockMetadata(discussion));
    }

    @Test
    void getDiffBlockMetadataLineDiffWithoutMetadataReturnsEmpty() throws Exception
    {
        Discussion discussion = mock(Discussion.class);
        DiscussionReference discussionReference = new DiscussionReference("changerequest", "discussion1");
        when(discussion.getReference()).thenReturn(discussionReference);

        FileDiffLocation fileDiffLocation = new FileDiffLocation("diff1", "xwiki:Space.Doc");
        LineDiffLocation lineDiffLocation = new LineDiffLocation(fileDiffLocation,
            LineDiffLocation.DiffDocumentPart.METADATA, "xwiki:Space.Doc", "content", 42,
            LineDiffLocation.LineChange.ADDED);
        ChangeRequestLineDiffReference lineDiffReference = new ChangeRequestLineDiffReference("CR1",
            lineDiffLocation);

        DiscussionContextReference contextRef = new DiscussionContextReference("changerequest", "ctx1");
        DiscussionContextEntityReference entityReference =
            new DiscussionContextEntityReference("changerequest-line_diff", "CR1__CRREF__nomethingrelevant");
        DiscussionContext lineDiffContext = new DiscussionContext(contextRef, "name", "desc", entityReference);

        when(this.discussionContextService.findByDiscussionReference(discussionReference))
            .thenReturn(List.of(lineDiffContext));
        when(this.discussionReferenceUtils.computeReferenceFromContext(lineDiffContext, null))
            .thenReturn(lineDiffReference);

        assertEquals(Optional.empty(), this.changeRequestDiscussionService.getDiffBlockMetadata(discussion));
    }

    @Test
    void getDiffBlockMetadataLineDiffWithMetadataReturnsBlock() throws Exception
    {
        Discussion discussion = mock(Discussion.class);
        DiscussionReference discussionReference = new DiscussionReference("changerequest", "discussion1");
        when(discussion.getReference()).thenReturn(discussionReference);

        FileDiffLocation fileDiffLocation = new FileDiffLocation("diff1", "xwiki:Space.Doc");
        LineDiffLocation lineDiffLocation = new LineDiffLocation(fileDiffLocation,
            LineDiffLocation.DiffDocumentPart.METADATA, "xwiki:Space.Doc", "content", 42,
            LineDiffLocation.LineChange.ADDED);
        ChangeRequestLineDiffReference lineDiffReference = new ChangeRequestLineDiffReference("CR1",
            lineDiffLocation);

        DiscussionContextReference contextRef = new DiscussionContextReference("changerequest", "ctx1");
        DiscussionContextEntityReference entityReference =
            new DiscussionContextEntityReference("changerequest-line_diff", "CR1__CRREF__nomethingrelevant");
        DiscussionContext lineDiffContext = new DiscussionContext(contextRef, "name", "desc", entityReference);
        lineDiffContext.getMetadata().put(ChangeRequestDiscussionService.DIFF_CONTEXT_METADATA_KEY,
            "{\"serialized\":true}");

        when(this.discussionContextService.findByDiscussionReference(discussionReference))
            .thenReturn(List.of(lineDiffContext));
        when(this.discussionReferenceUtils.computeReferenceFromContext(lineDiffContext, null))
            .thenReturn(lineDiffReference);

        UnifiedDiffBlock<String, Character> block = mock(UnifiedDiffBlock.class);
        when(this.changeRequestDiscussionDiffUtils.deserialize("{\"serialized\":true}")).thenReturn(block);

        Optional<ChangeRequestDiscussionDiffBlock> result =
            this.changeRequestDiscussionService.getDiffBlockMetadata(discussion);
        assertTrue(result.isPresent());
        assertEquals(block, result.get().getDiffBlock());
        assertEquals(lineDiffReference, result.get().getReference());
    }

    @Test
    void getDiffBlockMetadataDeserializationErrorWrapsException() throws Exception
    {
        Discussion discussion = mock(Discussion.class);
        DiscussionReference discussionReference = new DiscussionReference("changerequest", "discussion1");
        when(discussion.getReference()).thenReturn(discussionReference);

        FileDiffLocation fileDiffLocation = new FileDiffLocation("diff1", "xwiki:Space.Doc");
        LineDiffLocation lineDiffLocation = new LineDiffLocation(fileDiffLocation,
            LineDiffLocation.DiffDocumentPart.METADATA, "xwiki:Space.Doc", "content", 42,
            LineDiffLocation.LineChange.ADDED);
        ChangeRequestLineDiffReference lineDiffReference = new ChangeRequestLineDiffReference("CR1",
            lineDiffLocation);

        DiscussionContextReference contextRef = new DiscussionContextReference("changerequest", "ctx1");
        DiscussionContextEntityReference entityReference =
            new DiscussionContextEntityReference("changerequest-line_diff", "CR1__CRREF__nomethingrelevant");
        DiscussionContext lineDiffContext = new DiscussionContext(contextRef, "name", "desc", entityReference);
        lineDiffContext.getMetadata().put(ChangeRequestDiscussionService.DIFF_CONTEXT_METADATA_KEY,
            "{\"serialized\":true}");

        when(this.discussionContextService.findByDiscussionReference(discussionReference))
            .thenReturn(List.of(lineDiffContext));
        when(this.discussionReferenceUtils.computeReferenceFromContext(lineDiffContext, null))
            .thenReturn(lineDiffReference);

        JsonProcessingException jsonException = mock(JsonProcessingException.class);
        when(this.changeRequestDiscussionDiffUtils.deserialize("{\"serialized\":true}")).thenThrow(jsonException);

        ChangeRequestException exception = assertThrows(ChangeRequestException.class,
            () -> this.changeRequestDiscussionService.getDiffBlockMetadata(discussion));
        assertEquals(jsonException, exception.getCause());
    }

    @Test
    void refactorDiscussionFileReferenceUpdatesMatchingFileDiff() throws Exception
    {
        String changeRequestId = "CR1";
        DocumentReference source = new DocumentReference("xwiki", "Space", "Source");
        DocumentReference target = new DocumentReference("xwiki", "Space", "Target");

        when(this.stringEntityReferenceSerializer.serialize(source)).thenReturn("xwiki:Space.Source");
        when(this.stringEntityReferenceSerializer.serialize(target)).thenReturn("xwiki:Space.Target");

        ChangeRequestReference changeRequestReference = new ChangeRequestReference(changeRequestId);
        DiscussionContext crDiscussionContext = mock(DiscussionContext.class);
        DiscussionContextReference crContextRef = new DiscussionContextReference("changerequest", "crContextRef");
        when(crDiscussionContext.getReference()).thenReturn(crContextRef);
        when(this.changeRequestDiscussionFactory.getOrCreateContextFor(changeRequestReference))
            .thenReturn(crDiscussionContext);

        Discussion discussion = mock(Discussion.class);
        DiscussionReference discussionRef = new DiscussionReference("changerequest", "discussion1");
        when(discussion.getReference()).thenReturn(discussionRef);
        when(this.discussionService.findByDiscussionContexts(List.of(crContextRef)))
            .thenReturn(List.of(discussion));

        FileDiffLocation oldLocation = new FileDiffLocation("diff1", "xwiki:Space.Source");
        ChangeRequestFileDiffReference oldReference =
            new ChangeRequestFileDiffReference(changeRequestId, oldLocation);

        DiscussionContext fileDiffContext = mock(DiscussionContext.class);
        when(fileDiffContext.getName()).thenReturn("name1");
        when(fileDiffContext.getDescription()).thenReturn("desc1");
        when(this.discussionContextService.findByDiscussionReference(discussionRef))
            .thenReturn(List.of(fileDiffContext));
        when(this.discussionReferenceUtils.computeReferenceFromContext(fileDiffContext, null))
            .thenReturn(oldReference);

        FileDiffLocation newLocation = new FileDiffLocation("diff1", "xwiki:Space.Target");
        ChangeRequestFileDiffReference expectedNewReference =
            new ChangeRequestFileDiffReference(changeRequestId, newLocation);
        DiscussionContextEntityReference newEntityReference = new DiscussionContextEntityReference(
            "changerequest-file_diff", changeRequestId + "__CRREF__" + newLocation.getSerializedReference());
        when(this.changeRequestDiscussionFactory.createContextEntityReferenceFor(expectedNewReference))
            .thenReturn(newEntityReference);

        this.changeRequestDiscussionService.refactorDiscussionFileReference(changeRequestId, source, target, false);

        verify(this.discussionContextService).update(fileDiffContext, "name1", "desc1", newEntityReference);
    }

    @Test
    void refactorDiscussionFileReferenceDoesNotUpdateNonMatchingFileDiff() throws Exception
    {
        String changeRequestId = "CR1";
        DocumentReference source = new DocumentReference("xwiki", "Space", "Source");
        DocumentReference target = new DocumentReference("xwiki", "Space", "Target");

        when(this.stringEntityReferenceSerializer.serialize(source)).thenReturn("xwiki:Space.Source");
        when(this.stringEntityReferenceSerializer.serialize(target)).thenReturn("xwiki:Space.Target");

        ChangeRequestReference changeRequestReference = new ChangeRequestReference(changeRequestId);
        DiscussionContext crDiscussionContext = mock(DiscussionContext.class);
        DiscussionContextReference crContextRef = new DiscussionContextReference("changerequest", "crContextRef");
        when(crDiscussionContext.getReference()).thenReturn(crContextRef);
        when(this.changeRequestDiscussionFactory.getOrCreateContextFor(changeRequestReference))
            .thenReturn(crDiscussionContext);

        Discussion discussion = mock(Discussion.class);
        DiscussionReference discussionRef = new DiscussionReference("changerequest", "discussion1");
        when(discussion.getReference()).thenReturn(discussionRef);
        when(this.discussionService.findByDiscussionContexts(List.of(crContextRef)))
            .thenReturn(List.of(discussion));

        FileDiffLocation oldLocation = new FileDiffLocation("diff1", "xwiki:Space.Other");
        ChangeRequestFileDiffReference oldReference =
            new ChangeRequestFileDiffReference(changeRequestId, oldLocation);

        DiscussionContext fileDiffContext = mock(DiscussionContext.class);
        when(this.discussionContextService.findByDiscussionReference(discussionRef))
            .thenReturn(List.of(fileDiffContext));
        when(this.discussionReferenceUtils.computeReferenceFromContext(fileDiffContext, null))
            .thenReturn(oldReference);

        this.changeRequestDiscussionService.refactorDiscussionFileReference(changeRequestId, source, target, false);

        verify(this.discussionContextService, never()).update(any(), any(), any(), any());
    }

    @Test
    void refactorDiscussionFileReferenceUpdatesMatchingLineDiff() throws Exception
    {
        String changeRequestId = "CR1";
        DocumentReference source = new DocumentReference("xwiki", "Space", "Source");
        DocumentReference target = new DocumentReference("xwiki", "Space", "Target");

        when(this.stringEntityReferenceSerializer.serialize(source)).thenReturn("xwiki:Space.Source");
        when(this.stringEntityReferenceSerializer.serialize(target)).thenReturn("xwiki:Space.Target");

        ChangeRequestReference changeRequestReference = new ChangeRequestReference(changeRequestId);
        DiscussionContext crDiscussionContext = mock(DiscussionContext.class);
        DiscussionContextReference crContextRef = new DiscussionContextReference("changerequest", "crContextRef");
        when(crDiscussionContext.getReference()).thenReturn(crContextRef);
        when(this.changeRequestDiscussionFactory.getOrCreateContextFor(changeRequestReference))
            .thenReturn(crDiscussionContext);

        Discussion discussion = mock(Discussion.class);
        DiscussionReference discussionRef = new DiscussionReference("changerequest", "discussion1");
        when(discussion.getReference()).thenReturn(discussionRef);
        when(this.discussionService.findByDiscussionContexts(List.of(crContextRef)))
            .thenReturn(List.of(discussion));

        FileDiffLocation oldFileDiffLocation = new FileDiffLocation("diff1", "xwiki:Space.Source");
        LineDiffLocation oldLineDiffLocation = new LineDiffLocation(oldFileDiffLocation,
            LineDiffLocation.DiffDocumentPart.METADATA, "someOriginalEntityRef", "content", 5,
            LineDiffLocation.LineChange.ADDED);
        ChangeRequestLineDiffReference oldReference =
            new ChangeRequestLineDiffReference(changeRequestId, oldLineDiffLocation);

        DiscussionContext lineDiffContext = mock(DiscussionContext.class);
        when(lineDiffContext.getName()).thenReturn("name1");
        when(lineDiffContext.getDescription()).thenReturn("desc1");
        when(this.discussionContextService.findByDiscussionReference(discussionRef))
            .thenReturn(List.of(lineDiffContext));
        when(this.discussionReferenceUtils.computeReferenceFromContext(lineDiffContext, null))
            .thenReturn(oldReference);

        when(this.entityReferenceResolver.resolve("someOriginalEntityRef", null)).thenReturn(source);
        when(this.withTypeEntityReferenceSerializer.serialize(target)).thenReturn("fixedTargetRef");

        FileDiffLocation newFileDiffLocation = new FileDiffLocation("diff1", "xwiki:Space.Target");
        LineDiffLocation newLineDiffLocation = new LineDiffLocation(newFileDiffLocation,
            LineDiffLocation.DiffDocumentPart.METADATA, "fixedTargetRef", "content", 5,
            LineDiffLocation.LineChange.ADDED);
        ChangeRequestLineDiffReference expectedNewReference =
            new ChangeRequestLineDiffReference(changeRequestId, newLineDiffLocation);
        DiscussionContextEntityReference newEntityReference = new DiscussionContextEntityReference(
            "changerequest-line_diff",
            changeRequestId + "__CRREF__" + newLineDiffLocation.getSerializedReference());
        when(this.changeRequestDiscussionFactory.createContextEntityReferenceFor(expectedNewReference))
            .thenReturn(newEntityReference);

        this.changeRequestDiscussionService.refactorDiscussionFileReference(changeRequestId, source, target, false);

        verify(this.discussionContextService).update(lineDiffContext, "name1", "desc1", newEntityReference);
    }

    @Test
    void refactorDiscussionFileReferenceDoesNotUpdateGlobalReference() throws Exception
    {
        String changeRequestId = "CR1";
        DocumentReference source = new DocumentReference("xwiki", "Space", "Source");
        DocumentReference target = new DocumentReference("xwiki", "Space", "Target");

        when(this.stringEntityReferenceSerializer.serialize(source)).thenReturn("xwiki:Space.Source");
        when(this.stringEntityReferenceSerializer.serialize(target)).thenReturn("xwiki:Space.Target");

        ChangeRequestReference changeRequestReference = new ChangeRequestReference(changeRequestId);
        DiscussionContext crDiscussionContext = mock(DiscussionContext.class);
        DiscussionContextReference crContextRef = new DiscussionContextReference("changerequest", "crContextRef");
        when(crDiscussionContext.getReference()).thenReturn(crContextRef);
        when(this.changeRequestDiscussionFactory.getOrCreateContextFor(changeRequestReference))
            .thenReturn(crDiscussionContext);

        Discussion discussion = mock(Discussion.class);
        DiscussionReference discussionRef = new DiscussionReference("changerequest", "discussion1");
        when(discussion.getReference()).thenReturn(discussionRef);
        when(this.discussionService.findByDiscussionContexts(List.of(crContextRef)))
            .thenReturn(List.of(discussion));

        DiscussionContext commentContext = mock(DiscussionContext.class);
        when(this.discussionContextService.findByDiscussionReference(discussionRef))
            .thenReturn(List.of(commentContext));
        when(this.discussionReferenceUtils.computeReferenceFromContext(commentContext, null))
            .thenReturn(new ChangeRequestCommentReference(changeRequestId));

        this.changeRequestDiscussionService.refactorDiscussionFileReference(changeRequestId, source, target, false);

        verify(this.discussionContextService, never()).update(any(), any(), any(), any());
    }

    @Test
    void refactorDiscussionFileReferenceWrapsDiscussionException() throws Exception
    {
        String changeRequestId = "CR1";
        DocumentReference source = new DocumentReference("xwiki", "Space", "Source");
        DocumentReference target = new DocumentReference("xwiki", "Space", "Target");

        when(this.stringEntityReferenceSerializer.serialize(source)).thenReturn("xwiki:Space.Source");
        when(this.stringEntityReferenceSerializer.serialize(target)).thenReturn("xwiki:Space.Target");

        ChangeRequestReference changeRequestReference = new ChangeRequestReference(changeRequestId);
        DiscussionContext crDiscussionContext = mock(DiscussionContext.class);
        DiscussionContextReference crContextRef = new DiscussionContextReference("changerequest", "crContextRef");
        when(crDiscussionContext.getReference()).thenReturn(crContextRef);
        when(this.changeRequestDiscussionFactory.getOrCreateContextFor(changeRequestReference))
            .thenReturn(crDiscussionContext);

        Discussion discussion = mock(Discussion.class);
        DiscussionReference discussionRef = new DiscussionReference("changerequest", "discussion1");
        when(discussion.getReference()).thenReturn(discussionRef);
        when(this.discussionService.findByDiscussionContexts(List.of(crContextRef)))
            .thenReturn(List.of(discussion));

        FileDiffLocation oldLocation = new FileDiffLocation("diff1", "xwiki:Space.Source");
        ChangeRequestFileDiffReference oldReference =
            new ChangeRequestFileDiffReference(changeRequestId, oldLocation);

        DiscussionContext fileDiffContext = mock(DiscussionContext.class);
        when(this.discussionContextService.findByDiscussionReference(discussionRef))
            .thenReturn(List.of(fileDiffContext));
        when(this.discussionReferenceUtils.computeReferenceFromContext(fileDiffContext, null))
            .thenReturn(oldReference);

        when(this.changeRequestDiscussionFactory.createContextEntityReferenceFor(any()))
            .thenReturn(mock(DiscussionContextEntityReference.class));

        DiscussionException discussionException = new DiscussionException("error");
        doThrowUpdate(discussionException);

        assertThrows(ChangeRequestDiscussionException.class,
            () -> this.changeRequestDiscussionService.refactorDiscussionFileReference(changeRequestId, source,
                target, false));
    }

    private void doThrowUpdate(DiscussionException discussionException) throws DiscussionException
    {
        org.mockito.Mockito.doThrow(discussionException).when(this.discussionContextService)
            .update(any(), any(), any(), any());
    }

    @Test
    void moveDiscussionsGlobalReferenceCopiesToAllSplittedChangeRequests() throws Exception
    {
        ChangeRequest originalChangeRequest = mock(ChangeRequest.class);
        when(originalChangeRequest.getId()).thenReturn("CR1");

        ChangeRequest splitted1 = mock(ChangeRequest.class);
        when(splitted1.getId()).thenReturn("CR1-1");
        DocumentReference doc1 = new DocumentReference("xwiki", "Space", "Doc1");
        when(splitted1.getModifiedDocuments()).thenReturn(Set.of(doc1));
        when(this.stringEntityReferenceSerializer.serialize(doc1)).thenReturn("xwiki:Space.Doc1");

        ChangeRequest splitted2 = mock(ChangeRequest.class);
        when(splitted2.getId()).thenReturn("CR1-2");
        DocumentReference doc2 = new DocumentReference("xwiki", "Space", "Doc2");
        when(splitted2.getModifiedDocuments()).thenReturn(Set.of(doc2));
        when(this.stringEntityReferenceSerializer.serialize(doc2)).thenReturn("xwiki:Space.Doc2");

        List<ChangeRequest> splittedList = List.of(splitted1, splitted2);

        when(this.changeRequestDiscussionFactory.getOrCreateContextFor(any())).thenReturn(mock(
            DiscussionContext.class));

        ChangeRequestReference crReference = new ChangeRequestReference("CR1");
        when(this.changeRequestDiscussionFactory.isContextExistingFor(crReference)).thenReturn(true);
        DiscussionContext crContext = mock(DiscussionContext.class);
        DiscussionContextReference crContextRef = new DiscussionContextReference("changerequest", "crContextRef");
        when(crContext.getReference()).thenReturn(crContextRef);
        when(this.changeRequestDiscussionFactory.getOrCreateContextFor(crReference)).thenReturn(crContext);

        Discussion globalDiscussion = mock(Discussion.class);
        DiscussionReference globalDiscussionRef = new DiscussionReference("changerequest", "globalDiscussion");
        when(globalDiscussion.getReference()).thenReturn(globalDiscussionRef);
        when(this.discussionService.findByDiscussionContexts(Collections.singletonList(crContextRef)))
            .thenReturn(List.of(globalDiscussion));

        DiscussionContext commentContext = mock(DiscussionContext.class);
        when(this.discussionContextService.findByDiscussionReference(globalDiscussionRef))
            .thenReturn(List.of(commentContext));
        ChangeRequestCommentReference commentReference = new ChangeRequestCommentReference("CR1");
        when(this.discussionReferenceUtils.computeReferenceFromContext(commentContext, null))
            .thenReturn(commentReference);

        ChangeRequestCommentReference newCommentReference1 = new ChangeRequestCommentReference("CR1-1");
        ChangeRequestCommentReference newCommentReference2 = new ChangeRequestCommentReference("CR1-2");
        when(this.changeRequestDiscussionFactory.getOrCreateContextFor(newCommentReference1))
            .thenReturn(mock(DiscussionContext.class));
        when(this.changeRequestDiscussionFactory.getOrCreateContextFor(newCommentReference2))
            .thenReturn(mock(DiscussionContext.class));

        when(this.discussionReferenceUtils.getTitleTranslation(any(), any())).thenReturn("title");
        when(this.discussionReferenceUtils.getDescriptionTranslation(any(), any())).thenReturn("description");
        when(this.changeRequestDiscussionFactory.createDiscussionStoreConfigurationParametersFor(any()))
            .thenReturn(new DiscussionStoreConfigurationParameters());

        Discussion newDiscussion = mock(Discussion.class);
        when(this.discussionService.getOrCreate(eq(ChangeRequestDiscussionService.APPLICATION_HINT), any(), any(),
            any(), any())).thenReturn(newDiscussion);

        this.changeRequestDiscussionService.moveDiscussions(originalChangeRequest, splittedList);

        verify(this.changeRequestDiscussionFactory).copyMessages(eq(globalDiscussion), any(Discussion.class),
            eq(newCommentReference1));
        verify(this.changeRequestDiscussionFactory).copyMessages(eq(globalDiscussion), any(Discussion.class),
            eq(newCommentReference2));
    }

    @Test
    void moveDiscussionsFileDiffReferenceMovesToMatchingChangeRequest() throws Exception
    {
        ChangeRequest originalChangeRequest = mock(ChangeRequest.class);
        when(originalChangeRequest.getId()).thenReturn("CR1");

        ChangeRequest splittedMatching = mock(ChangeRequest.class);
        when(splittedMatching.getId()).thenReturn("CR1-1");
        DocumentReference matchingDoc = new DocumentReference("xwiki", "Space", "Doc1");
        when(splittedMatching.getModifiedDocuments()).thenReturn(Set.of(matchingDoc));
        when(this.stringEntityReferenceSerializer.serialize(matchingDoc)).thenReturn("xwiki:Space.Doc1");

        ChangeRequest splittedOther = mock(ChangeRequest.class);
        when(splittedOther.getId()).thenReturn("CR1-2");
        DocumentReference otherDoc = new DocumentReference("xwiki", "Space", "Doc2");
        when(splittedOther.getModifiedDocuments()).thenReturn(Set.of(otherDoc));
        when(this.stringEntityReferenceSerializer.serialize(otherDoc)).thenReturn("xwiki:Space.Doc2");

        List<ChangeRequest> splittedList = List.of(splittedMatching, splittedOther);

        when(this.changeRequestDiscussionFactory.getOrCreateContextFor(any())).thenReturn(mock(
            DiscussionContext.class));

        ChangeRequestReference crReference = new ChangeRequestReference("CR1");
        when(this.changeRequestDiscussionFactory.isContextExistingFor(crReference)).thenReturn(true);
        DiscussionContext crContext = mock(DiscussionContext.class);
        DiscussionContextReference crContextRef = new DiscussionContextReference("changerequest", "crContextRef");
        when(crContext.getReference()).thenReturn(crContextRef);
        when(this.changeRequestDiscussionFactory.getOrCreateContextFor(crReference)).thenReturn(crContext);

        Discussion fileDiffDiscussion = mock(Discussion.class);
        DiscussionReference fileDiffDiscussionRef = new DiscussionReference("changerequest", "fileDiffDiscussion");
        when(fileDiffDiscussion.getReference()).thenReturn(fileDiffDiscussionRef);
        when(this.discussionService.findByDiscussionContexts(Collections.singletonList(crContextRef)))
            .thenReturn(List.of(fileDiffDiscussion));

        FileDiffLocation fileDiffLocation = new FileDiffLocation("diff1", "xwiki:Space.Doc1");
        ChangeRequestFileDiffReference fileDiffReference = new ChangeRequestFileDiffReference("CR1",
            fileDiffLocation);

        DiscussionContext fileDiffContext = mock(DiscussionContext.class);
        when(this.discussionContextService.findByDiscussionReference(fileDiffDiscussionRef))
            .thenReturn(List.of(fileDiffContext));
        when(this.discussionReferenceUtils.computeReferenceFromContext(fileDiffContext, null))
            .thenReturn(fileDiffReference);

        ChangeRequestFileDiffReference newReference = new ChangeRequestFileDiffReference("CR1-1", fileDiffLocation);
        Discussion newDiscussion = mock(Discussion.class);

        when(this.discussionReferenceUtils.getTitleTranslation(any(), any())).thenReturn("title");
        when(this.discussionReferenceUtils.getDescriptionTranslation(any(), any())).thenReturn("description");
        when(this.changeRequestDiscussionFactory.createDiscussionStoreConfigurationParametersFor(any()))
            .thenReturn(new DiscussionStoreConfigurationParameters());
        when(this.changeRequestDiscussionFactory.getOrCreateContextFor(newReference))
            .thenReturn(mock(DiscussionContext.class));
        when(this.discussionService.getOrCreate(eq(ChangeRequestDiscussionService.APPLICATION_HINT), any(), any(),
            any(), any())).thenReturn(newDiscussion);

        this.changeRequestDiscussionService.moveDiscussions(originalChangeRequest, splittedList);

        verify(this.changeRequestDiscussionFactory).copyMessages(fileDiffDiscussion, newDiscussion, newReference);
        verify(this.changeRequestDiscussionFactory, never()).getOrCreateContextFor(
            new ChangeRequestFileDiffReference("CR1-2", fileDiffLocation));
    }

    @Test
    void moveDiscussionsLineDiffReferenceMovesToMatchingChangeRequest() throws Exception
    {
        ChangeRequest originalChangeRequest = mock(ChangeRequest.class);
        when(originalChangeRequest.getId()).thenReturn("CR1");

        ChangeRequest splittedMatching = mock(ChangeRequest.class);
        when(splittedMatching.getId()).thenReturn("CR1-1");
        DocumentReference matchingDoc = new DocumentReference("xwiki", "Space", "Doc1");
        when(splittedMatching.getModifiedDocuments()).thenReturn(Set.of(matchingDoc));
        when(this.stringEntityReferenceSerializer.serialize(matchingDoc)).thenReturn("xwiki:Space.Doc1");

        List<ChangeRequest> splittedList = List.of(splittedMatching);

        when(this.changeRequestDiscussionFactory.getOrCreateContextFor(any())).thenReturn(mock(
            DiscussionContext.class));

        ChangeRequestReference crReference = new ChangeRequestReference("CR1");
        when(this.changeRequestDiscussionFactory.isContextExistingFor(crReference)).thenReturn(true);
        DiscussionContext crContext = mock(DiscussionContext.class);
        DiscussionContextReference crContextRef = new DiscussionContextReference("changerequest", "crContextRef");
        when(crContext.getReference()).thenReturn(crContextRef);
        when(this.changeRequestDiscussionFactory.getOrCreateContextFor(crReference)).thenReturn(crContext);

        Discussion lineDiffDiscussion = mock(Discussion.class);
        DiscussionReference lineDiffDiscussionRef = new DiscussionReference("changerequest", "lineDiffDiscussion");
        when(lineDiffDiscussion.getReference()).thenReturn(lineDiffDiscussionRef);
        when(this.discussionService.findByDiscussionContexts(Collections.singletonList(crContextRef)))
            .thenReturn(List.of(lineDiffDiscussion));

        FileDiffLocation fileDiffLocation = new FileDiffLocation("diff1", "xwiki:Space.Doc1");
        LineDiffLocation lineDiffLocation = new LineDiffLocation(fileDiffLocation,
            LineDiffLocation.DiffDocumentPart.METADATA, "xwiki:Space.Doc1", "content", 3,
            LineDiffLocation.LineChange.ADDED);
        ChangeRequestLineDiffReference lineDiffReference = new ChangeRequestLineDiffReference("CR1",
            lineDiffLocation);

        DiscussionContext lineDiffContext = mock(DiscussionContext.class);
        when(this.discussionContextService.findByDiscussionReference(lineDiffDiscussionRef))
            .thenReturn(List.of(lineDiffContext));
        when(this.discussionReferenceUtils.computeReferenceFromContext(lineDiffContext, null))
            .thenReturn(lineDiffReference);

        ChangeRequestLineDiffReference newReference = new ChangeRequestLineDiffReference("CR1-1", lineDiffLocation);
        Discussion newDiscussion = mock(Discussion.class);

        when(this.discussionReferenceUtils.getTitleTranslation(any(), any())).thenReturn("title");
        when(this.discussionReferenceUtils.getDescriptionTranslation(any(), any())).thenReturn("description");
        when(this.changeRequestDiscussionFactory.createDiscussionStoreConfigurationParametersFor(any()))
            .thenReturn(new DiscussionStoreConfigurationParameters());
        when(this.changeRequestDiscussionFactory.getOrCreateContextFor(newReference))
            .thenReturn(mock(DiscussionContext.class));
        when(this.discussionService.getOrCreate(eq(ChangeRequestDiscussionService.APPLICATION_HINT), any(), any(),
            any(), any())).thenReturn(newDiscussion);

        this.changeRequestDiscussionService.moveDiscussions(originalChangeRequest, splittedList);

        verify(this.changeRequestDiscussionFactory).copyMessages(lineDiffDiscussion, newDiscussion, newReference);
    }

    @Test
    void moveDiscussionsNoMatchingChangeRequestLogsErrorAndSkipsMove() throws Exception
    {
        ChangeRequest originalChangeRequest = mock(ChangeRequest.class);
        when(originalChangeRequest.getId()).thenReturn("CR1");

        ChangeRequest splittedOther = mock(ChangeRequest.class);
        when(splittedOther.getId()).thenReturn("CR1-2");
        DocumentReference otherDoc = new DocumentReference("xwiki", "Space", "Doc2");
        when(splittedOther.getModifiedDocuments()).thenReturn(Set.of(otherDoc));
        when(this.stringEntityReferenceSerializer.serialize(otherDoc)).thenReturn("xwiki:Space.Doc2");

        List<ChangeRequest> splittedList = List.of(splittedOther);

        ChangeRequestReference crReference = new ChangeRequestReference("CR1");
        when(this.changeRequestDiscussionFactory.isContextExistingFor(crReference)).thenReturn(true);
        DiscussionContext crContext = mock(DiscussionContext.class);
        DiscussionContextReference crContextRef = new DiscussionContextReference("changerequest", "crContextRef");
        when(crContext.getReference()).thenReturn(crContextRef);
        when(this.changeRequestDiscussionFactory.getOrCreateContextFor(crReference)).thenReturn(crContext);

        Discussion fileDiffDiscussion = mock(Discussion.class);
        DiscussionReference fileDiffDiscussionRef = new DiscussionReference("changerequest", "fileDiffDiscussion");
        when(fileDiffDiscussion.getReference()).thenReturn(fileDiffDiscussionRef);
        when(this.discussionService.findByDiscussionContexts(Collections.singletonList(crContextRef)))
            .thenReturn(List.of(fileDiffDiscussion));

        FileDiffLocation fileDiffLocation = new FileDiffLocation("diff1", "xwiki:Space.DocNotFound");
        ChangeRequestFileDiffReference fileDiffReference = new ChangeRequestFileDiffReference("CR1",
            fileDiffLocation);

        DiscussionContext fileDiffContext = mock(DiscussionContext.class);
        when(this.discussionContextService.findByDiscussionReference(fileDiffDiscussionRef))
            .thenReturn(List.of(fileDiffContext));
        when(this.discussionReferenceUtils.computeReferenceFromContext(fileDiffContext, null))
            .thenReturn(fileDiffReference);

        this.changeRequestDiscussionService.moveDiscussions(originalChangeRequest, splittedList);

        verify(this.changeRequestDiscussionFactory, never()).copyMessages(any(), any(), any());
        verify(this.changeRequestDiscussionFactory, never()).getOrCreateContextFor(
            new ChangeRequestFileDiffReference("CR1-2", fileDiffLocation));
        assertEquals(1, logCapture.size());
        assertEquals("Cannot find change request associated with file [xwiki:Space.DocNotFound] "
            + "the discussion might be lost", logCapture.getMessage(0));
    }
}
