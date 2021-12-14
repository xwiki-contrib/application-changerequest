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

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.xwiki.contrib.changerequest.discussions.ChangeRequestDiscussionException;
import org.xwiki.contrib.changerequest.discussions.ChangeRequestDiscussionService;
import org.xwiki.contrib.changerequest.discussions.references.ChangeRequestCommentReference;
import org.xwiki.contrib.changerequest.discussions.references.ChangeRequestFileDiffReference;
import org.xwiki.contrib.changerequest.discussions.references.ChangeRequestLineDiffReference;
import org.xwiki.contrib.changerequest.discussions.references.ChangeRequestReference;
import org.xwiki.contrib.changerequest.discussions.references.ChangeRequestReviewReference;
import org.xwiki.contrib.changerequest.discussions.references.ChangeRequestReviewsReference;
import org.xwiki.contrib.changerequest.discussions.references.difflocation.FileDiffLocation;
import org.xwiki.contrib.changerequest.discussions.references.difflocation.LineDiffLocation;
import org.xwiki.contrib.discussions.DiscussionContextService;
import org.xwiki.contrib.discussions.DiscussionStoreConfigurationParameters;
import org.xwiki.contrib.discussions.MessageService;
import org.xwiki.contrib.discussions.domain.DiscussionContext;
import org.xwiki.contrib.discussions.domain.references.DiscussionContextEntityReference;
import org.xwiki.store.merge.MergeDocumentResult;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ChangeRequestDiscussionFactory}.
 *
 * @version $Id$
 * @since 0.7
 */
@ComponentTest
class ChangeRequestDiscussionFactoryTest
{
    @InjectMockComponents
    private ChangeRequestDiscussionFactory factory;

    @MockComponent
    private ChangeRequestDiscussionReferenceUtils discussionReferenceUtils;

    @MockComponent
    private DiscussionContextService discussionContextService;

    @MockComponent
    private MessageService messageService;

    @Test
    void createDiscussionStoreConfigurationParametersFor()
    {
        ChangeRequestCommentReference reference = mock(ChangeRequestCommentReference.class);
        when(reference.getChangeRequestId()).thenReturn("someId");

        DiscussionStoreConfigurationParameters parameters = new DiscussionStoreConfigurationParameters();
        parameters.put(DefaultChangeRequestDiscussionStoreConfiguration.CHANGE_REQUEST_ID_PARAMETER_KEY, "someId");
        assertEquals(parameters, this.factory.createDiscussionStoreConfigurationParametersFor(reference));
    }

    @Test
    void getOrCreateContextFor_withChangeRequestReference() throws ChangeRequestDiscussionException
    {
        String changeRequestId = "crId42";
        String prefix = ChangeRequestDiscussionReferenceUtils.DISCUSSION_CONTEXT_TRANSLATION_PREFIX;

        DiscussionStoreConfigurationParameters parameters = new DiscussionStoreConfigurationParameters();
        parameters.put(DefaultChangeRequestDiscussionStoreConfiguration.CHANGE_REQUEST_ID_PARAMETER_KEY,
            changeRequestId);

        ChangeRequestReference changeRequestReference = new ChangeRequestReference(changeRequestId);

        DiscussionContextEntityReference contextEntityReference =
            new DiscussionContextEntityReference("changerequest-change_request", changeRequestId);

        when(this.discussionReferenceUtils.getTitleTranslation(prefix, changeRequestReference)).thenReturn("title42");
        when(this.discussionReferenceUtils.getDescriptionTranslation(prefix, changeRequestReference))
            .thenReturn("desc42");

        DiscussionContext expectedContext = mock(DiscussionContext.class);
        when(this.discussionContextService.getOrCreate(
            ChangeRequestDiscussionService.APPLICATION_HINT,
            "title42",
            "desc42",
            contextEntityReference,
            parameters
        )).thenReturn(Optional.of(expectedContext));

        assertSame(expectedContext, this.factory.getOrCreateContextFor(changeRequestReference));
    }

    @Test
    void getOrCreateContextFor_withChangeRequestCommentReference() throws ChangeRequestDiscussionException
    {
        String changeRequestId = "crId42";
        String prefix = ChangeRequestDiscussionReferenceUtils.DISCUSSION_CONTEXT_TRANSLATION_PREFIX;

        DiscussionStoreConfigurationParameters parameters = new DiscussionStoreConfigurationParameters();
        parameters.put(DefaultChangeRequestDiscussionStoreConfiguration.CHANGE_REQUEST_ID_PARAMETER_KEY,
            changeRequestId);

        ChangeRequestCommentReference changeRequestReference = new ChangeRequestCommentReference(changeRequestId);

        DiscussionContextEntityReference contextEntityReference =
            new DiscussionContextEntityReference("changerequest-change_request_comment", changeRequestId);

        when(this.discussionReferenceUtils.getTitleTranslation(prefix, changeRequestReference)).thenReturn("title42");
        when(this.discussionReferenceUtils.getDescriptionTranslation(prefix, changeRequestReference))
            .thenReturn("desc42");

        DiscussionContext expectedContext = mock(DiscussionContext.class);
        when(this.discussionContextService.getOrCreate(
            ChangeRequestDiscussionService.APPLICATION_HINT,
            "title42",
            "desc42",
            contextEntityReference,
            parameters
        )).thenReturn(Optional.of(expectedContext));

        assertSame(expectedContext, this.factory.getOrCreateContextFor(changeRequestReference));
    }

    @Test
    void getOrCreateContextFor_withChangeRequestReviewsReference() throws ChangeRequestDiscussionException
    {
        String changeRequestId = "crId42";
        String prefix = ChangeRequestDiscussionReferenceUtils.DISCUSSION_CONTEXT_TRANSLATION_PREFIX;

        DiscussionStoreConfigurationParameters parameters = new DiscussionStoreConfigurationParameters();
        parameters.put(DefaultChangeRequestDiscussionStoreConfiguration.CHANGE_REQUEST_ID_PARAMETER_KEY,
            changeRequestId);

        ChangeRequestReviewsReference changeRequestReference = new ChangeRequestReviewsReference(changeRequestId);

        DiscussionContextEntityReference contextEntityReference =
            new DiscussionContextEntityReference("changerequest-reviews", changeRequestId);

        when(this.discussionReferenceUtils.getTitleTranslation(prefix, changeRequestReference)).thenReturn("title42");
        when(this.discussionReferenceUtils.getDescriptionTranslation(prefix, changeRequestReference))
            .thenReturn("desc42");

        DiscussionContext expectedContext = mock(DiscussionContext.class);
        when(this.discussionContextService.getOrCreate(
            ChangeRequestDiscussionService.APPLICATION_HINT,
            "title42",
            "desc42",
            contextEntityReference,
            parameters
        )).thenReturn(Optional.of(expectedContext));

        assertSame(expectedContext, this.factory.getOrCreateContextFor(changeRequestReference));
    }

    @Test
    void getOrCreateContextFor_withChangeRequestReviewReference() throws ChangeRequestDiscussionException
    {
        String changeRequestId = "crId42";
        String prefix = ChangeRequestDiscussionReferenceUtils.DISCUSSION_CONTEXT_TRANSLATION_PREFIX;

        DiscussionStoreConfigurationParameters parameters = new DiscussionStoreConfigurationParameters();
        parameters.put(DefaultChangeRequestDiscussionStoreConfiguration.CHANGE_REQUEST_ID_PARAMETER_KEY,
            changeRequestId);

        ChangeRequestReviewReference changeRequestReference =
            new ChangeRequestReviewReference("review_424", changeRequestId);

        DiscussionContextEntityReference contextEntityReference =
            new DiscussionContextEntityReference("changerequest-review",
                changeRequestId + ChangeRequestDiscussionFactory.CR_ID_REF_ID_SEPARATOR + "review_424");

        when(this.discussionReferenceUtils.getTitleTranslation(prefix, changeRequestReference)).thenReturn("title42");
        when(this.discussionReferenceUtils.getDescriptionTranslation(prefix, changeRequestReference))
            .thenReturn("desc42");

        DiscussionContext expectedContext = mock(DiscussionContext.class);
        when(this.discussionContextService.getOrCreate(
            ChangeRequestDiscussionService.APPLICATION_HINT,
            "title42",
            "desc42",
            contextEntityReference,
            parameters
        )).thenReturn(Optional.of(expectedContext));

        assertSame(expectedContext, this.factory.getOrCreateContextFor(changeRequestReference));
    }

    @Test
    void getOrCreateContextFor_withChangeRequestFileDiffReference() throws ChangeRequestDiscussionException
    {
        String changeRequestId = "crId42";
        String prefix = ChangeRequestDiscussionReferenceUtils.DISCUSSION_CONTEXT_TRANSLATION_PREFIX;

        DiscussionStoreConfigurationParameters parameters = new DiscussionStoreConfigurationParameters();
        parameters.put(DefaultChangeRequestDiscussionStoreConfiguration.CHANGE_REQUEST_ID_PARAMETER_KEY,
            changeRequestId);

        ChangeRequestFileDiffReference changeRequestReference =
            new ChangeRequestFileDiffReference(changeRequestId, new FileDiffLocation("diff4858", "xwiki:Main.WebHome"));

        DiscussionContextEntityReference contextEntityReference =
            new DiscussionContextEntityReference("changerequest-file_diff",
                changeRequestId +
                ChangeRequestDiscussionFactory.CR_ID_REF_ID_SEPARATOR + "xwiki:Main.WebHome/diff4858");

        when(this.discussionReferenceUtils.getTitleTranslation(prefix, changeRequestReference)).thenReturn("title42");
        when(this.discussionReferenceUtils.getDescriptionTranslation(prefix, changeRequestReference))
            .thenReturn("desc42");

        DiscussionContext expectedContext = mock(DiscussionContext.class);
        when(this.discussionContextService.getOrCreate(
            ChangeRequestDiscussionService.APPLICATION_HINT,
            "title42",
            "desc42",
            contextEntityReference,
            parameters
        )).thenReturn(Optional.of(expectedContext));

        assertSame(expectedContext, this.factory.getOrCreateContextFor(changeRequestReference));
    }

    @Test
    void getOrCreateContextFor_withChangeRequestLineDiffReference() throws ChangeRequestDiscussionException
    {
        String changeRequestId = "crId42";
        String prefix = ChangeRequestDiscussionReferenceUtils.DISCUSSION_CONTEXT_TRANSLATION_PREFIX;

        DiscussionStoreConfigurationParameters parameters = new DiscussionStoreConfigurationParameters();
        parameters.put(DefaultChangeRequestDiscussionStoreConfiguration.CHANGE_REQUEST_ID_PARAMETER_KEY,
            changeRequestId);

        FileDiffLocation fileDiffLocation = new FileDiffLocation("diff4858", "xwiki:Main.WebHome");
        LineDiffLocation lineDiffLocation = new LineDiffLocation(fileDiffLocation,
            LineDiffLocation.DiffDocumentPart.XOBJECT,
            "xwiki:Main.WebHome^XWiki.StyleSheetExtension[0]",
            "block234",
            48,
            LineDiffLocation.LineChange.REMOVED);
        ChangeRequestLineDiffReference changeRequestReference = new ChangeRequestLineDiffReference(
            changeRequestId,
            lineDiffLocation);

        DiscussionContextEntityReference contextEntityReference =
            new DiscussionContextEntityReference("changerequest-line_diff",
                changeRequestId + ChangeRequestDiscussionFactory.CR_ID_REF_ID_SEPARATOR +
                "xwiki:Main.WebHome/diff4858/XOBJECT/xwiki:Main.WebHome^XWiki.StyleSheetExtension[0]/"
                    + "block234/REMOVED/48");

        when(this.discussionReferenceUtils.getTitleTranslation(prefix, changeRequestReference)).thenReturn("title42");
        when(this.discussionReferenceUtils.getDescriptionTranslation(prefix, changeRequestReference))
            .thenReturn("desc42");

        DiscussionContext expectedContext = mock(DiscussionContext.class);
        when(this.discussionContextService.getOrCreate(
            ChangeRequestDiscussionService.APPLICATION_HINT,
            "title42",
            "desc42",
            contextEntityReference,
            parameters
        )).thenReturn(Optional.of(expectedContext));

        assertSame(expectedContext, this.factory.getOrCreateContextFor(changeRequestReference));
    }
}
