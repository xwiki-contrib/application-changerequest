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

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestManager;
import org.xwiki.contrib.changerequest.ChangeRequestReview;
import org.xwiki.contrib.changerequest.ChangeRequestStatus;
import org.xwiki.contrib.changerequest.storage.ReviewStorageManager;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Test
    void addReview(MockitoComponentManager mockitoComponentManager) throws Exception
    {
        UserReference userReference = mock(UserReference.class);
        when(this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE)).thenReturn(userReference);
        when(this.componentManagerProvider.get()).thenReturn(mockitoComponentManager);
        ChangeRequestAuthorizationScriptService authorizationScriptService =
            mock(ChangeRequestAuthorizationScriptService.class);
        mockitoComponentManager.registerComponent(ScriptService.class, "changerequest.authorization",
            authorizationScriptService);
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(authorizationScriptService.isAuthorizedToReview(changeRequest)).thenReturn(true);
        ChangeRequestReview review = mock(ChangeRequestReview.class);
        when(this.changeRequestManager.addReview(changeRequest, userReference, false, null)).thenReturn(review);
        assertEquals(review, this.scriptService.addReview(changeRequest, false));
        verify(authorizationScriptService).isAuthorizedToReview(changeRequest);
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

        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(review.getChangeRequest()).thenReturn(changeRequest);
        when(changeRequest.getStatus()).thenReturn(ChangeRequestStatus.DRAFT);
        assertFalse(this.scriptService.canEditReview(review));

        when(review.getAuthor()).thenReturn(userReference);
        assertFalse(this.scriptService.canEditReview(review));

        when(review.isLastFromAuthor()).thenReturn(true);
        assertTrue(this.scriptService.canEditReview(review));

        when(changeRequest.getStatus()).thenReturn(ChangeRequestStatus.MERGED);
        assertFalse(this.scriptService.canEditReview(review));
    }

    @Test
    void setReviewValidity() throws ChangeRequestException
    {
        UserReference userReference = mock(UserReference.class);
        when(this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE)).thenReturn(userReference);
        ChangeRequestReview review = mock(ChangeRequestReview.class);
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(review.getChangeRequest()).thenReturn(changeRequest);
        when(changeRequest.getStatus()).thenReturn(ChangeRequestStatus.READY_FOR_REVIEW);
        UserReference anotherUser = mock(UserReference.class);
        when(review.getAuthor()).thenReturn(anotherUser);
        assertFalse(this.scriptService.setReviewValidity(review, true));
        assertFalse(this.scriptService.setReviewValidity(review, false));
        verify(review, never()).setValid(true);
        verify(review, never()).setValid(false);
        verify(review, never()).setSaved(false);
        verify(this.reviewStorageManager, never()).save(review);

        when(review.getAuthor()).thenReturn(userReference);
        when(review.isLastFromAuthor()).thenReturn(true);
        assertTrue(this.scriptService.setReviewValidity(review, true));
        verify(review).setValid(true);
        verify(review).setSaved(false);
        verify(this.reviewStorageManager).save(review);
    }
}
