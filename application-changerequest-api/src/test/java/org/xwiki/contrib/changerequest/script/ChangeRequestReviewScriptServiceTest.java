package org.xwiki.contrib.changerequest.script;/*
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

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestManager;
import org.xwiki.contrib.changerequest.ChangeRequestReview;
import org.xwiki.contrib.changerequest.storage.ReviewStorageManager;
import org.xwiki.observation.ObservationManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ChangeRequestReviewScriptService}.
 *
 * @version $Id$
 * @since 0.8
 */
@ComponentTest
class ChangeRequestReviewScriptServiceTest
{
    @InjectMockComponents
    private ChangeRequestReviewScriptService scriptService;

    @MockComponent
    private ChangeRequestManager changeRequestManager;

    @MockComponent
    private UserReferenceResolver<CurrentUserReference> currentUserReferenceResolver;

    @MockComponent
    private ReviewStorageManager reviewStorageManager;

    @MockComponent
    private ObservationManager observationManager;

    @Test
    void isAuthorizedToReview() throws ChangeRequestException
    {
        UserReference userReference = mock(UserReference.class);
        when(this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE)).thenReturn(userReference);
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(this.changeRequestManager.isAuthorizedToReview(userReference, changeRequest)).thenReturn(true);
        assertTrue(this.scriptService.isAuthorizedToReview(changeRequest));
        verify(this.changeRequestManager).isAuthorizedToReview(userReference, changeRequest);
    }

    @Test
    void addReview() throws ChangeRequestException
    {
        UserReference userReference = mock(UserReference.class);
        when(this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE)).thenReturn(userReference);
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(this.changeRequestManager.isAuthorizedToReview(userReference, changeRequest)).thenReturn(true);
        ChangeRequestReview review = new ChangeRequestReview(changeRequest, false, userReference);
        doAnswer(invocationOnMock -> {
            ChangeRequestReview review1 = invocationOnMock.getArgument(0);
            review.setReviewDate(review1.getReviewDate());
            return null;
        }).when(this.reviewStorageManager).save(any());
        assertEquals(review, this.scriptService.addReview(changeRequest, false));
        verify(this.reviewStorageManager).save(review);
    }

    @Test
    void getReview()
    {
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        String reviewId = "some id";
        when(changeRequest.getReviews()).thenReturn(Collections.emptyList());
        assertEquals(Optional.empty(), this.scriptService.getReview(changeRequest, reviewId));

        when(changeRequest.getReviews()).thenReturn(Arrays.asList(
            mock(ChangeRequestReview.class),
            mock(ChangeRequestReview.class),
            mock(ChangeRequestReview.class)
        ));
        assertEquals(Optional.empty(), this.scriptService.getReview(changeRequest, reviewId));

        ChangeRequestReview review = mock(ChangeRequestReview.class);
        when(changeRequest.getReviews()).thenReturn(Arrays.asList(
            mock(ChangeRequestReview.class),
            mock(ChangeRequestReview.class),
            review,
            mock(ChangeRequestReview.class)
        ));
        when(review.getId()).thenReturn(reviewId);
        assertEquals(Optional.of(review), this.scriptService.getReview(changeRequest, reviewId));
    }

    @Test
    void canEditReview()
    {
        UserReference userReference = mock(UserReference.class);
        when(this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE)).thenReturn(userReference);
        ChangeRequestReview review = mock(ChangeRequestReview.class);
        when(review.getAuthor()).thenReturn(mock(UserReference.class));
        assertFalse(this.scriptService.canEditReview(review));

        when(review.getAuthor()).thenReturn(userReference);
        assertTrue(this.scriptService.canEditReview(review));
    }

    @Test
    void setReviewValidity() throws ChangeRequestException
    {
        UserReference userReference = mock(UserReference.class);
        when(this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE)).thenReturn(userReference);
        ChangeRequestReview review = mock(ChangeRequestReview.class);
        UserReference anotherUser = mock(UserReference.class);
        when(review.getAuthor()).thenReturn(anotherUser);
        assertFalse(this.scriptService.setReviewValidity(review, true));
        assertFalse(this.scriptService.setReviewValidity(review, false));
        verify(review, never()).setValid(true);
        verify(review, never()).setValid(false);
        verify(review, never()).setSaved(false);
        verify(this.reviewStorageManager, never()).save(review);

        when(review.getAuthor()).thenReturn(userReference);
        assertTrue(this.scriptService.setReviewValidity(review, true));
        verify(review).setValid(true);
        verify(review).setSaved(false);
        verify(this.reviewStorageManager).save(review);
    }
}
