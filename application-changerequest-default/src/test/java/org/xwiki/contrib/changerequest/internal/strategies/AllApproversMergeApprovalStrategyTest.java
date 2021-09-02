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
package org.xwiki.contrib.changerequest.internal.strategies;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.contrib.changerequest.ApproversManager;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestReview;
import org.xwiki.contrib.changerequest.MergeApprovalStrategy;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link AllApproversMergeApprovalStrategy}.
 *
 * @version $Id$
 * @since 0.5
 */
@ComponentTest
class AllApproversMergeApprovalStrategyTest
{
    @InjectMockComponents
    private AllApproversMergeApprovalStrategy strategy;

    @MockComponent
    @Named(OnlyApprovedMergeApprovalStrategy.NAME)
    private MergeApprovalStrategy fallbackStrategy;

    @MockComponent
    private ApproversManager<ChangeRequest> changeRequestApproversManager;

    @MockComponent
    private ContextualLocalizationManager contextualLocalizationManager;

    @Test
    void canBeMerged() throws ChangeRequestException
    {
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(this.changeRequestApproversManager.getAllApprovers(changeRequest, true))
            .thenReturn(Collections.emptySet());
        when(fallbackStrategy.canBeMerged(changeRequest)).thenReturn(true);
        assertTrue(this.strategy.canBeMerged(changeRequest));
        verify(this.fallbackStrategy).canBeMerged(changeRequest);

        UserReference user1 = mock(UserReference.class);
        UserReference user2 = mock(UserReference.class);
        when(this.changeRequestApproversManager.getAllApprovers(changeRequest, true))
            .thenReturn(new HashSet<>(Arrays.asList(user1, user2)));

        ChangeRequestReview review1 = mock(ChangeRequestReview.class);

        when(changeRequest.getReviews()).thenReturn(Collections.singletonList(review1));
        when(review1.isApproved()).thenReturn(true);
        when(review1.isValid()).thenReturn(true);
        when(review1.getAuthor()).thenReturn(user1);
        assertFalse(this.strategy.canBeMerged(changeRequest));

        ChangeRequestReview review2 = mock(ChangeRequestReview.class);
        when(changeRequest.getReviews()).thenReturn(Arrays.asList(review1, review2));

        when(review2.isApproved()).thenReturn(true);
        when(review2.isValid()).thenReturn(true);
        when(review2.getAuthor()).thenReturn(user2);
        assertTrue(this.strategy.canBeMerged(changeRequest));

        when(review2.isApproved()).thenReturn(false);
        assertFalse(this.strategy.canBeMerged(changeRequest));

        ChangeRequestReview review3 = mock(ChangeRequestReview.class);
        when(changeRequest.getReviews()).thenReturn(Arrays.asList(review1, review2, review3));

        when(review3.isApproved()).thenReturn(true);
        when(review3.isValid()).thenReturn(true);
        when(review3.getAuthor()).thenReturn(user2);
        assertTrue(this.strategy.canBeMerged(changeRequest));

        when(review3.isValid()).thenReturn(false);
        assertFalse(this.strategy.canBeMerged(changeRequest));
    }

    @Test
    void getStatus() throws ChangeRequestException
    {
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(this.changeRequestApproversManager.getAllApprovers(changeRequest, true))
            .thenReturn(Collections.emptySet());
        when(this.fallbackStrategy.getStatus(changeRequest)).thenReturn("Fallback status");
        assertEquals("Fallback status", this.strategy.getStatus(changeRequest));

        UserReference user1 = mock(UserReference.class);
        when(this.changeRequestApproversManager.getAllApprovers(changeRequest, true))
            .thenReturn(Collections.singleton(user1));

        ChangeRequestReview review1 = mock(ChangeRequestReview.class);

        when(changeRequest.getReviews()).thenReturn(Collections.singletonList(review1));
        when(review1.isApproved()).thenReturn(true);
        when(review1.isValid()).thenReturn(true);
        when(review1.getAuthor()).thenReturn(user1);

        String successTranslationKey = "changerequest.strategies.allapprovers.success";
        String failureTranslationKey = "changerequest.strategies.allapprovers.failure";
        when(this.contextualLocalizationManager.getTranslationPlain(successTranslationKey)).thenReturn("Success");
        when(this.contextualLocalizationManager.getTranslationPlain(failureTranslationKey)).thenReturn("Failure");

        assertEquals("Success", this.strategy.getStatus(changeRequest));

        when(review1.isApproved()).thenReturn(false);
        assertEquals("Failure", this.strategy.getStatus(changeRequest));
    }
}
