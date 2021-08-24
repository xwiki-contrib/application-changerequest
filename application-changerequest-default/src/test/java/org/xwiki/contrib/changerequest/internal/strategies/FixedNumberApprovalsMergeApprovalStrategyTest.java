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

import org.junit.jupiter.api.Test;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestReview;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link FixedNumberApprovalsMergeApprovalStrategy}.
 *
 * @version $Id$
 * @since 0.4
 */
@ComponentTest
public class FixedNumberApprovalsMergeApprovalStrategyTest
{
    @InjectMockComponents
    private FixedNumberApprovalsMergeApprovalStrategy strategy;

    @MockComponent
    private ContextualLocalizationManager contextualLocalizationManager;

    @Test
    void canBeMerged()
    {
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(changeRequest.getReviews()).thenReturn(Collections.emptyList());
        assertFalse(strategy.canBeMerged(changeRequest));

        ChangeRequestReview review1 = mock(ChangeRequestReview.class);
        ChangeRequestReview review2 = mock(ChangeRequestReview.class);
        ChangeRequestReview review3 = mock(ChangeRequestReview.class);
        ChangeRequestReview review4 = mock(ChangeRequestReview.class);
        when(changeRequest.getReviews()).thenReturn(Arrays.asList(review1, review2, review3, review4));

        when(review1.isApproved()).thenReturn(true);
        when(review2.isApproved()).thenReturn(false);
        when(review3.isApproved()).thenReturn(true);
        when(review4.isApproved()).thenReturn(false);

        when(review1.isValid()).thenReturn(false);
        when(review2.isValid()).thenReturn(false);
        when(review3.isValid()).thenReturn(false);
        when(review4.isValid()).thenReturn(false);
        assertFalse(strategy.canBeMerged(changeRequest));

        when(review1.isValid()).thenReturn(true);
        when(review2.isValid()).thenReturn(true);
        when(review3.isValid()).thenReturn(true);
        when(review4.isValid()).thenReturn(true);
        assertFalse(strategy.canBeMerged(changeRequest));

        when(review4.isApproved()).thenReturn(true);
        assertTrue(strategy.canBeMerged(changeRequest));

        when(review4.isValid()).thenReturn(false);
        assertFalse(strategy.canBeMerged(changeRequest));

        when(changeRequest.getReviews()).thenReturn(Collections.singletonList(review1));
        assertFalse(strategy.canBeMerged(changeRequest));
    }

    @Test
    void getName()
    {
        assertEquals("fixednumberapprovals", strategy.getName());
    }

    @Test
    void getDescription()
    {
        when(this.contextualLocalizationManager
            .getTranslationPlain("changerequest.strategies.fixednumberapprovals.description")).thenReturn("foobar");
        assertEquals("foobar", strategy.getDescription());
    }

    @Test
    void getStatus()
    {
        ChangeRequest changeRequest = mock(ChangeRequest.class);

        when(this.contextualLocalizationManager
            .getTranslationPlain("changerequest.strategies.fixednumberapprovals.status.success", 3))
            .thenReturn("success4242");
        when(this.contextualLocalizationManager
            .getTranslationPlain("changerequest.strategies.fixednumberapprovals.status.failure", 3, 3))
            .thenReturn("failurefoobar");

        when(changeRequest.getReviews()).thenReturn(Collections.emptyList());
        assertEquals("failurefoobar", strategy.getStatus(changeRequest));

        ChangeRequestReview review = mock(ChangeRequestReview.class);
        when(review.isApproved()).thenReturn(true);
        when(review.isValid()).thenReturn(true);
        when(changeRequest.getReviews()).thenReturn(Arrays.asList(review, review, review));
        assertEquals("success4242", strategy.getStatus(changeRequest));
    }
}
