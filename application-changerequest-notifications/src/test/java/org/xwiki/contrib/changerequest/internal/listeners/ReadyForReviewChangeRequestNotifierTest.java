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
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.contrib.changerequest.ApproversManager;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestStatus;
import org.xwiki.contrib.changerequest.events.ChangeRequestStatusChangedEvent;
import org.xwiki.contrib.changerequest.internal.ChangeRequestRecordableEventNotifier;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestReadyForReviewTargetableEvent;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceSerializer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ReadyForReviewChangeRequestNotifier}.
 *
 * @version $Id$
 * @since 0.11
 */
@ComponentTest
class ReadyForReviewChangeRequestNotifierTest
{
    @InjectMockComponents
    private ReadyForReviewChangeRequestNotifier changeRequestNotifier;

    @MockComponent
    private ChangeRequestStorageManager changeRequestStorageManager;

    @MockComponent
    private ApproversManager<ChangeRequest> changeRequestApproversManager;

    @MockComponent
    private UserReferenceSerializer<String> userReferenceSerializer;

    @MockComponent
    private ChangeRequestRecordableEventNotifier changeRequestRecordableEventNotifier;

    @MockComponent
    private DocumentReferenceResolver<ChangeRequest> changeRequestDocumentReferenceResolver;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @Test
    void onEvent() throws Exception
    {
        ChangeRequestStatusChangedEvent event = mock(ChangeRequestStatusChangedEvent.class);
        this.changeRequestNotifier.onEvent(event, null, null);
        this.changeRequestNotifier.onEvent(event, null, new ChangeRequestStatus[0]);
        this.changeRequestNotifier.onEvent(event, null, new ChangeRequestStatus[] {
            ChangeRequestStatus.CLOSED, ChangeRequestStatus.DRAFT
        });
        verifyNoInteractions(this.changeRequestRecordableEventNotifier);
        verifyNoInteractions(this.changeRequestStorageManager);

        String changeRequestId = "crId";
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(this.changeRequestStorageManager.load(changeRequestId)).thenReturn(Optional.of(changeRequest));

        UserReference approver1 = mock(UserReference.class);
        UserReference approver2 = mock(UserReference.class);
        UserReference approver3 = mock(UserReference.class);

        when(this.userReferenceSerializer.serialize(approver1)).thenReturn("approver1");
        when(this.userReferenceSerializer.serialize(approver2)).thenReturn("approver2");
        when(this.userReferenceSerializer.serialize(approver3)).thenReturn("approver3");
        when(this.changeRequestApproversManager.getAllApprovers(changeRequest, true))
            .thenReturn(new HashSet<>(Arrays.asList(approver1, approver2, approver3)));

        DocumentReference crDocRef = mock(DocumentReference.class);
        DocumentModelBridge documentModelBridge = mock(DocumentModelBridge.class);
        when(this.changeRequestDocumentReferenceResolver.resolve(changeRequest)).thenReturn(crDocRef);
        when(this.documentAccessBridge.getTranslatedDocumentInstance(crDocRef)).thenReturn(documentModelBridge);

        Set<String> expectedTargets = new HashSet<>(Arrays.asList(
            "approver1",
            "approver2",
            "approver3"
        ));

        doAnswer(invocationOnMock -> {
            ChangeRequestReadyForReviewTargetableEvent targetableEvent = invocationOnMock.getArgument(0);
            assertEquals(expectedTargets, targetableEvent.getTarget());
            return null;
        }).when(this.changeRequestRecordableEventNotifier).notifyChangeRequestRecordableEvent(
            any(ChangeRequestReadyForReviewTargetableEvent.class), any());

        this.changeRequestNotifier.onEvent(event, changeRequestId, new ChangeRequestStatus[] {
            ChangeRequestStatus.DRAFT, ChangeRequestStatus.READY_FOR_REVIEW
        });
        verify(this.changeRequestRecordableEventNotifier).notifyChangeRequestRecordableEvent(
            any(ChangeRequestReadyForReviewTargetableEvent.class),
            eq(documentModelBridge));
    }
}
