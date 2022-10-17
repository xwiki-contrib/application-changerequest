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
package org.xwiki.contrib.changerequest.internal.listeners;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Named;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.xwiki.contrib.changerequest.ApproversManager;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestReview;
import org.xwiki.contrib.changerequest.ChangeRequestStatus;
import org.xwiki.contrib.changerequest.events.ApproversUpdatedEvent;
import org.xwiki.contrib.changerequest.internal.UserReferenceConverter;
import org.xwiki.contrib.changerequest.internal.storage.ChangeRequestXClassInitializer;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.contrib.changerequest.storage.ReviewStorageManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserManager;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.user.group.GroupManager;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ApproversUpdatedListener}.
 *
 * @version $Id$
 * @since 0.14
 */
@ComponentTest
class ApproversUpdatedListenerTest
{
    @InjectMockComponents
    private ApproversUpdatedListener listener;

    @MockComponent
    private ChangeRequestStorageManager changeRequestStorageManager;

    @MockComponent
    private ApproversManager<ChangeRequest> changeRequestApproversManager;

    @MockComponent
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @MockComponent
    private GroupManager groupManager;

    @MockComponent
    private UserReferenceConverter userReferenceConverter;

    @MockComponent
    private ReviewStorageManager reviewStorageManager;

    @MockComponent
    private UserManager userManager;

    @MockComponent
    @Named("current")
    private UserReferenceResolver<String> userReferenceResolver;

    @Test
    void onEventFromChangeRequest()
    {
        ApproversUpdatedEvent event = mock(ApproversUpdatedEvent.class);
        XWikiDocument sourceDoc = mock(XWikiDocument.class);
        when(sourceDoc.getXObject(ChangeRequestXClassInitializer.CHANGE_REQUEST_XCLASS))
            .thenReturn(mock(BaseObject.class));
        this.listener.onEvent(event, sourceDoc, null);
        verifyNoInteractions(this.changeRequestStorageManager);
    }

    @Test
    void onEvent() throws Exception
    {
        ApproversUpdatedEvent event = mock(ApproversUpdatedEvent.class);
        XWikiDocument sourceDoc = mock(XWikiDocument.class);
        DocumentReference sourceDocRef = mock(DocumentReference.class);
        when(sourceDoc.getDocumentReference()).thenReturn(sourceDocRef);
        Set<String> previousApprovers = new HashSet<>(Arrays.asList("user1", "user2", "groupA"));
        Set<String> nextApprovers = new HashSet<>(Arrays.asList("user2", "groupB"));
        Pair<Set<String>, Set<String>> data = Pair.of(previousApprovers, nextApprovers);

        ChangeRequest changeRequest1 = mock(ChangeRequest.class);
        ChangeRequest changeRequest2 = mock(ChangeRequest.class);
        ChangeRequest changeRequest3 = mock(ChangeRequest.class);
        ChangeRequest changeRequest4 = mock(ChangeRequest.class);
        when(this.changeRequestStorageManager.findChangeRequestTargeting(sourceDocRef)).thenReturn(Arrays.asList(
            changeRequest1,
            changeRequest2,
            changeRequest3,
            changeRequest4
        ));

        when(changeRequest1.getStatus()).thenReturn(ChangeRequestStatus.DRAFT);
        when(changeRequest2.getStatus()).thenReturn(ChangeRequestStatus.READY_FOR_REVIEW);
        when(changeRequest3.getStatus()).thenReturn(ChangeRequestStatus.READY_FOR_REVIEW);
        when(changeRequest4.getStatus()).thenReturn(ChangeRequestStatus.MERGED);

        when(this.changeRequestApproversManager.wasManuallyEdited(changeRequest3)).thenReturn(true);

        // Review 1 from CR1 is invalid and done by user 1
        // Review 2 from CR1 is valid and done by user1 -> needs to be invalidated
        // Review 3 from CR1 is valid and done by user3
        ChangeRequestReview cr1Review1 = mock(ChangeRequestReview.class, "cr1Review1");
        ChangeRequestReview cr1Review2 = mock(ChangeRequestReview.class, "cr1Review2");
        ChangeRequestReview cr1Review3 = mock(ChangeRequestReview.class, "cr1Review3");

        when(changeRequest1.getReviews()).thenReturn(Arrays.asList(cr1Review1, cr1Review2, cr1Review3));

        // Review 1 from CR2 is valid and done by user 3 on behalf of user 1 -> needs to be invalidated
        // Review 2 from CR2 is valid and done by user 2
        // Review 3 from CR2 is valid and done by user 4 who belongs to group A -> needs to be invalidated
        ChangeRequestReview cr2Review1 = mock(ChangeRequestReview.class, "cr2Review1");
        ChangeRequestReview cr2Review2 = mock(ChangeRequestReview.class, "cr2Review2");
        ChangeRequestReview cr2Review3 = mock(ChangeRequestReview.class, "cr2Review3");

        when(changeRequest2.getReviews()).thenReturn(Arrays.asList(cr2Review1, cr2Review2, cr2Review3));

        UserReference user1Ref = mock(UserReference.class);
        UserReference user2Ref = mock(UserReference.class);
        UserReference user3Ref = mock(UserReference.class);
        UserReference user4Ref = mock(UserReference.class);

        DocumentReference user1DocRef = mock(DocumentReference.class);
        DocumentReference user2DocRef = mock(DocumentReference.class);
        DocumentReference user3DocRef = mock(DocumentReference.class);
        DocumentReference user4DocRef = mock(DocumentReference.class);

        when(this.userReferenceConverter.convert(user1Ref)).thenReturn(user1DocRef);
        when(this.userReferenceConverter.convert(user2Ref)).thenReturn(user2DocRef);
        when(this.userReferenceConverter.convert(user3Ref)).thenReturn(user3DocRef);
        when(this.userReferenceConverter.convert(user4Ref)).thenReturn(user4DocRef);

        when(cr1Review1.isValid()).thenReturn(false);
        when(cr1Review1.getAuthor()).thenReturn(user1Ref);

        when(cr1Review2.isValid()).thenReturn(true);
        when(cr1Review2.getAuthor()).thenReturn(user1Ref);

        when(cr1Review3.isValid()).thenReturn(true);
        when(cr1Review3.getAuthor()).thenReturn(user3Ref);

        when(cr2Review1.isValid()).thenReturn(true);
        when(cr2Review1.getAuthor()).thenReturn(user3Ref);
        when(cr2Review1.getOriginalApprover()).thenReturn(user1Ref);

        when(cr2Review2.isValid()).thenReturn(true);
        when(cr2Review2.getAuthor()).thenReturn(user2Ref);

        when(cr2Review3.isValid()).thenReturn(true);
        when(cr2Review3.getAuthor()).thenReturn(user4Ref);

        when(this.documentReferenceResolver.resolve("user1")).thenReturn(user1DocRef);
        DocumentReference groupADocRef = mock(DocumentReference.class);
        when(this.documentReferenceResolver.resolve("groupA")).thenReturn(groupADocRef);

        when(this.groupManager.getMembers(groupADocRef, true)).thenReturn(Collections.singletonList(user4DocRef));

        DocumentReference groupBDocRef = mock(DocumentReference.class);
        when(this.documentReferenceResolver.resolve("groupB")).thenReturn(groupBDocRef);
        when(this.userReferenceResolver.resolve("user2")).thenReturn(user2Ref);

        when(this.userManager.exists(user2Ref)).thenReturn(true);

        this.listener.onEvent(event, sourceDoc, data);
        verify(cr1Review1, never()).setValid(false);
        verify(cr1Review1, never()).setSaved(false);
        verify(this.reviewStorageManager, never()).save(cr1Review1);

        verify(cr1Review2).setValid(false);
        verify(cr1Review2).setSaved(false);
        verify(this.reviewStorageManager).save(cr1Review2);

        verify(cr1Review3, never()).setValid(false);
        verify(cr1Review3, never()).setSaved(false);
        verify(this.reviewStorageManager, never()).save(cr1Review3);

        verify(cr2Review1).setValid(false);
        verify(cr2Review1).setSaved(false);
        verify(this.reviewStorageManager).save(cr2Review1);

        verify(cr2Review2, never()).setValid(false);
        verify(cr2Review2, never()).setSaved(false);
        verify(this.reviewStorageManager, never()).save(cr2Review2);

        verify(cr2Review3).setValid(false);
        verify(cr2Review3).setSaved(false);
        verify(this.reviewStorageManager).save(cr2Review3);

        verify(this.changeRequestApproversManager).setUsersApprovers(Collections.singleton(user2Ref), changeRequest1);
        verify(this.changeRequestApproversManager).setUsersApprovers(Collections.singleton(user2Ref), changeRequest2);

        verify(this.changeRequestApproversManager).setGroupsApprovers(Collections.singleton(groupBDocRef),
            changeRequest1);
        verify(this.changeRequestApproversManager).setGroupsApprovers(Collections.singleton(groupBDocRef),
            changeRequest2);
        verify(changeRequest3, never()).getReviews();
        verify(changeRequest4, never()).getReviews();
        verify(this.changeRequestApproversManager, never()).wasManuallyEdited(changeRequest4);
    }
}