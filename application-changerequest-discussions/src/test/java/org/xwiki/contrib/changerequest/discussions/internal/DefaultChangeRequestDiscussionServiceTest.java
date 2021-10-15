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

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.xwiki.contrib.changerequest.discussions.ChangeRequestDiscussionException;
import org.xwiki.contrib.changerequest.discussions.ChangeRequestDiscussionService;
import org.xwiki.contrib.changerequest.discussions.references.AbstractChangeRequestDiscussionContextReference;
import org.xwiki.contrib.changerequest.discussions.references.ChangeRequestFileDiffReference;
import org.xwiki.contrib.changerequest.discussions.references.ChangeRequestLineDiffReference;
import org.xwiki.contrib.changerequest.discussions.references.ChangeRequestReference;
import org.xwiki.contrib.discussions.DiscussionContextService;
import org.xwiki.contrib.discussions.DiscussionService;
import org.xwiki.contrib.discussions.domain.Discussion;
import org.xwiki.contrib.discussions.domain.DiscussionContext;
import org.xwiki.contrib.discussions.domain.references.DiscussionContextEntityReference;
import org.xwiki.contrib.discussions.domain.references.DiscussionContextReference;
import org.xwiki.contrib.discussions.domain.references.DiscussionReference;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.store.merge.MergeDocumentResult;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
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
    private ContextualLocalizationManager localizationManager;

    @Test
    void getReferencesFrom() throws ChangeRequestDiscussionException
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
}
