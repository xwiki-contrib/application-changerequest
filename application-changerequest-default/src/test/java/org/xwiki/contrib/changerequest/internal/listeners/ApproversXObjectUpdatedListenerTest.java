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
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.xwiki.contrib.changerequest.events.ApproversUpdatedEvent;
import org.xwiki.contrib.changerequest.internal.approvers.ApproversXClassInitializer;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.remote.RemoteObservationManagerContext;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ApproversXObjectUpdatedListener}.
 *
 * @version $Id$
 * @since 0.10
 */
@ComponentTest
class ApproversXObjectUpdatedListenerTest
{
    @InjectMockComponents
    private ApproversXObjectUpdatedListener listener;

    @MockComponent
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @MockComponent
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @MockComponent
    private RemoteObservationManagerContext remoteObservationManagerContext;

    @MockComponent
    private ObservationManager observationManager;

    @Test
    void onEvent()
    {
        when(this.remoteObservationManagerContext.isRemoteState()).thenReturn(true);
        this.listener.onEvent(null, null, null);
        verifyNoInteractions(this.observationManager);

        when(this.remoteObservationManagerContext.isRemoteState()).thenReturn(false);
        XWikiDocument sourceDoc = mock(XWikiDocument.class);
        XWikiDocument originalDoc = mock(XWikiDocument.class);
        when(sourceDoc.getOriginalDocument()).thenReturn(originalDoc);

        BaseObject currentObj = mock(BaseObject.class);
        BaseObject previousObj = mock(BaseObject.class);
        when(sourceDoc.getXObject(ApproversXClassInitializer.APPROVERS_XCLASS)).thenReturn(currentObj);
        when(originalDoc.getXObject(ApproversXClassInitializer.APPROVERS_XCLASS)).thenReturn(previousObj);

        String user1 = "user1";
        String user2 = "user2";
        String user3 = "user3";

        String group1 = "group1";
        String group2 = "group2";

        DocumentReference userRef1 = mock(DocumentReference.class);
        DocumentReference userRef2 = mock(DocumentReference.class);
        DocumentReference userRef3 = mock(DocumentReference.class);

        DocumentReference groupRef1 = mock(DocumentReference.class);
        DocumentReference groupRef2 = mock(DocumentReference.class);

        when(this.documentReferenceResolver.resolve(user1)).thenReturn(userRef1);
        when(this.documentReferenceResolver.resolve(user2)).thenReturn(userRef2);
        when(this.documentReferenceResolver.resolve(user3)).thenReturn(userRef3);

        when(this.documentReferenceResolver.resolve(group1)).thenReturn(groupRef1);
        when(this.documentReferenceResolver.resolve(group2)).thenReturn(groupRef2);

        String xwikiPrefix = "XWiki.";
        when(this.entityReferenceSerializer.serialize(userRef1)).thenReturn(xwikiPrefix + user1);
        when(this.entityReferenceSerializer.serialize(userRef2)).thenReturn(xwikiPrefix + user2);
        when(this.entityReferenceSerializer.serialize(userRef3)).thenReturn(xwikiPrefix + user3);

        when(this.entityReferenceSerializer.serialize(groupRef1)).thenReturn(xwikiPrefix + group1);
        when(this.entityReferenceSerializer.serialize(groupRef2)).thenReturn(xwikiPrefix + group2);

        when(currentObj.getLargeStringValue(ApproversXClassInitializer.USERS_APPROVERS_PROPERTY)).thenReturn(user1);
        when(previousObj.getLargeStringValue(ApproversXClassInitializer.USERS_APPROVERS_PROPERTY))
            .thenReturn(StringUtils.join(Arrays.asList(user2, "", user3, null),
                ApproversXClassInitializer.SEPARATOR_CHARACTER));

        when(currentObj.getLargeStringValue(ApproversXClassInitializer.GROUPS_APPROVERS_PROPERTY))
            .thenReturn(StringUtils.join(Arrays.asList(group1, group2),
                ApproversXClassInitializer.SEPARATOR_CHARACTER));
        when(previousObj.getLargeStringValue(ApproversXClassInitializer.GROUPS_APPROVERS_PROPERTY)).thenReturn(null);

        Set<String> expectedSet = new HashSet<>(Arrays.asList(
            xwikiPrefix + user1,
            xwikiPrefix + user2,
            xwikiPrefix + user3,
            xwikiPrefix + group1,
            xwikiPrefix + group2
        ));

        doAnswer(invocationOnMock -> {
            XWikiDocument doc = invocationOnMock.getArgument(1);
            Set<String> targetSet = invocationOnMock.getArgument(2);
            assertSame(sourceDoc, doc);
            assertEquals(expectedSet, targetSet);
            return null;
        }).when(this.observationManager).notify(any(ApproversUpdatedEvent.class), any(), any());

        this.listener.onEvent(null, sourceDoc, null);
        verify(this.observationManager).notify(any(ApproversUpdatedEvent.class), any(), any());
    }
}
