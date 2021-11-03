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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestConfiguration;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestRightsManager;
import org.xwiki.contrib.changerequest.ChangeRequestStatus;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.contrib.rights.RightUpdatedEvent;
import org.xwiki.contrib.rights.SecurityRuleDiff;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.authorization.ReadableSecurityRule;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link RightsUpdatedListener}.
 *
 * @version $Id$
 * @since 0.7
 */
@ComponentTest
class RightsUpdatedListenerTest
{
    @InjectMockComponents
    private RightsUpdatedListener listener;

    @MockComponent
    private ChangeRequestStorageManager changeRequestStorageManager;

    @MockComponent
    private ChangeRequestConfiguration configuration;

    @MockComponent
    private ChangeRequestRightsManager changeRequestRightsManager;

    @Test
    void processLocalEvents() throws ChangeRequestException
    {
        RightUpdatedEvent event = new RightUpdatedEvent();
        EntityReference source = new WikiReference("foo");
        List<SecurityRuleDiff> data = new ArrayList<>();

        this.listener.processLocalEvent(event, source, data);

        verifyNoInteractions(this.configuration);
        verifyNoInteractions(this.changeRequestStorageManager);
        verifyNoInteractions(this.changeRequestRightsManager);

        SpaceReference changeRequestSpaceReference = mock(SpaceReference.class);
        when(this.configuration.getChangeRequestSpaceLocation()).thenReturn(changeRequestSpaceReference);
        source = mock(SpaceReference.class);
        when(source.hasParent(changeRequestSpaceReference)).thenReturn(true);

        this.listener.processLocalEvent(event, source, data);

        verifyNoInteractions(this.changeRequestStorageManager);
        verifyNoInteractions(this.changeRequestRightsManager);

        source = new SpaceReference("Something", new WikiReference("foo"));

        DocumentReference expectedRef = new DocumentReference("WebHome", (SpaceReference) source);
        when(this.changeRequestStorageManager.findChangeRequestTargeting(expectedRef))
            .thenReturn(Collections.emptyList());
        this.listener.processLocalEvent(event, source, data);
        verifyNoInteractions(this.changeRequestRightsManager);

        ChangeRequest changeRequest1 = mock(ChangeRequest.class);
        ChangeRequest changeRequest2 = mock(ChangeRequest.class);
        ChangeRequest changeRequest3 = mock(ChangeRequest.class);

        when(this.changeRequestStorageManager.findChangeRequestTargeting(expectedRef))
            .thenReturn(Arrays.asList(changeRequest1, changeRequest2, changeRequest3));
        this.listener.processLocalEvent(event, source, data);
        verifyNoInteractions(this.changeRequestRightsManager);

        SecurityRuleDiff diff1 = mock(SecurityRuleDiff.class);
        SecurityRuleDiff diff2 = mock(SecurityRuleDiff.class);
        SecurityRuleDiff diff3 = mock(SecurityRuleDiff.class);
        data = Arrays.asList(diff1, diff2, diff3);
        when(this.changeRequestStorageManager.findChangeRequestTargeting(expectedRef))
            .thenReturn(Collections.emptyList());

        this.listener.processLocalEvent(event, source, data);
        verifyNoInteractions(this.changeRequestRightsManager);

        when(this.changeRequestStorageManager.findChangeRequestTargeting(expectedRef))
            .thenReturn(Arrays.asList(changeRequest1, changeRequest2, changeRequest3));

        // rule subjects
        DocumentReference user1 = mock(DocumentReference.class);
        DocumentReference user2 = mock(DocumentReference.class);

        DocumentReference groupA = mock(DocumentReference.class);
        DocumentReference groupB = mock(DocumentReference.class);

        ReadableSecurityRule ruleDiff1 = mock(ReadableSecurityRule.class);
        ReadableSecurityRule ruleDiff2 = mock(ReadableSecurityRule.class);

        ReadableSecurityRule rule1Diff3 = mock(ReadableSecurityRule.class);
        ReadableSecurityRule rule2Diff3 = mock(ReadableSecurityRule.class);

        when(diff1.getCurrentRule()).thenReturn(ruleDiff1);
        when(diff2.getPreviousRule()).thenReturn(ruleDiff2);
        when(diff3.getPreviousRule()).thenReturn(rule1Diff3);
        when(diff3.getCurrentRule()).thenReturn(rule2Diff3);

        when(ruleDiff1.match(Right.VIEW)).thenReturn(true);
        when(ruleDiff2.match(Right.VIEW)).thenReturn(false);
        when(rule1Diff3.match(Right.VIEW)).thenReturn(true);
        when(rule2Diff3.match(Right.VIEW)).thenReturn(false);

        // should never be used
        when(ruleDiff2.getGroups()).thenReturn(Collections.singletonList(mock(DocumentReference.class)));

        when(ruleDiff1.getGroups()).thenReturn(Collections.singletonList(groupA));
        when(rule1Diff3.getUsers()).thenReturn(Arrays.asList(user1, user2));
        when(rule2Diff3.getUsers()).thenReturn(Collections.singletonList(user1));
        when(rule2Diff3.getGroups()).thenReturn(Collections.singletonList(groupB));

        when(changeRequest1.getStatus()).thenReturn(ChangeRequestStatus.READY_FOR_REVIEW);
        when(changeRequest2.getStatus()).thenReturn(ChangeRequestStatus.MERGED);
        when(changeRequest3.getStatus()).thenReturn(ChangeRequestStatus.CLOSED);

        when(this.changeRequestRightsManager.isViewAccessStillConsistent(changeRequest1,
            Stream.of(user1, user2, groupA, groupB).collect(Collectors.toSet())))
            .thenReturn(false);
        ChangeRequest splitted1 = mock(ChangeRequest.class);
        ChangeRequest splitted2 = mock(ChangeRequest.class);
        when(this.changeRequestStorageManager.split(changeRequest1)).thenReturn(Arrays.asList(splitted1, splitted2));
        when(splitted2.getModifiedDocuments()).thenReturn(Collections.singleton(expectedRef));

        this.listener.processLocalEvent(event, source, data);
        verify(this.changeRequestRightsManager).isViewAccessStillConsistent(changeRequest1,
            Stream.of(user1, user2, groupA, groupB).collect(Collectors.toSet()));
        verify(this.changeRequestRightsManager).copyViewRights(changeRequest3, expectedRef);
        verify(this.changeRequestRightsManager).copyViewRights(splitted2, expectedRef);
        verify(this.changeRequestStorageManager).split(changeRequest1);
        verify(this.changeRequestRightsManager, never()).copyViewRights(eq(changeRequest2), any());
        verify(this.changeRequestStorageManager, never()).split(changeRequest2);
    }
}
