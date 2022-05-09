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

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.contrib.changerequest.ChangeRequestConfiguration;
import org.xwiki.contrib.changerequest.events.ChangeRequestMergingEvent;
import org.xwiki.contrib.changerequest.internal.approvers.ApproversXClassInitializer;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.observation.ObservationContext;
import org.xwiki.observation.remote.RemoteObservationManagerContext;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.group.GroupManager;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.UserUpdatingDocumentEvent;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ApproversXObjectUpdatingListener}.
 *
 * @version $Id$
 * @since 0.8
 */
@ComponentTest
class ApproversXobjectUpdatingListenerTest
{
    private static final String TEST_USER = "testUser";

    @InjectMockComponents
    private ApproversXObjectUpdatingListener listener;

    @MockComponent
    private GroupManager groupManager;

    @MockComponent
    private DocumentReferenceResolver<String> stringDocumentReferenceResolver;

    @MockComponent
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @MockComponent
    private ObservationContext observationContext;

    @MockComponent
    private RemoteObservationManagerContext remoteObservationManagerContext;

    @MockComponent
    private ChangeRequestConfiguration configuration;

    private XWikiDocument source;
    private DocumentReference user;
    private UserUpdatingDocumentEvent event;

    @BeforeEach
    void setup()
    {
        this.source = mock(XWikiDocument.class);
        this.user = mock(DocumentReference.class);
        this.event = mock(UserUpdatingDocumentEvent.class);
        when(this.event.getUserReference()).thenReturn(this.user);
        when(this.stringDocumentReferenceResolver.resolve(TEST_USER)).thenReturn(this.user);
    }

    @Test
    void onEvent_admin()
    {
        when(this.contextualAuthorizationManager.hasAccess(Right.ADMIN)).thenReturn(true);
        this.listener.onEvent(this.event, this.source, null);
        verifyNoInteractions(this.source);
        verifyNoInteractions(this.configuration);
    }

    @Test
    void onEvent_inChangeRequestMerging()
    {
        when(this.contextualAuthorizationManager.hasAccess(Right.ADMIN)).thenReturn(false);
        when(this.observationContext.isIn(any(ChangeRequestMergingEvent.class))).thenReturn(true);
        this.listener.onEvent(this.event, this.source, null);
        verifyNoInteractions(this.source);
    }

    @Test
    void onEvent_remote()
    {
        when(this.contextualAuthorizationManager.hasAccess(Right.ADMIN)).thenReturn(false);
        when(this.observationContext.isIn(any(ChangeRequestMergingEvent.class))).thenReturn(false);
        when(this.remoteObservationManagerContext.isRemoteState()).thenReturn(true);
        this.listener.onEvent(this.event, this.source, null);
        verifyNoInteractions(this.source);
    }

    @Test
    void onEvent_needsCancelBecauseUserListed()
    {
        when(this.contextualAuthorizationManager.hasAccess(Right.ADMIN)).thenReturn(false);
        when(this.observationContext.isIn(any(ChangeRequestMergingEvent.class))).thenReturn(false);
        when(this.remoteObservationManagerContext.isRemoteState()).thenReturn(false);

        XWikiDocument originalDocument = mock(XWikiDocument.class);
        when(this.source.getOriginalDocument()).thenReturn(originalDocument);

        when(originalDocument.getXObjects(ApproversXClassInitializer.APPROVERS_XCLASS)).thenReturn(
            Collections.emptyList());

        BaseObject approverObject = mock(BaseObject.class);
        when(this.source.getXObjects(ApproversXClassInitializer.APPROVERS_XCLASS))
            .thenReturn(Collections.singletonList(approverObject));
        when(approverObject.getLargeStringValue(ApproversXClassInitializer.USERS_APPROVERS_PROPERTY))
            .thenReturn(TEST_USER);
        this.listener.onEvent(this.event, this.source, null);
        verify(this.source).removeXObject(approverObject);
    }

    @Test
    void onEvent_needsCancelMinimumApproversNotRespected()
    {
        when(this.contextualAuthorizationManager.hasAccess(Right.ADMIN)).thenReturn(false);
        when(this.observationContext.isIn(any(ChangeRequestMergingEvent.class))).thenReturn(false);
        when(this.remoteObservationManagerContext.isRemoteState()).thenReturn(false);

        when(this.configuration.getMinimumApprovers()).thenReturn(3);
        XWikiDocument originalDocument = mock(XWikiDocument.class);
        when(this.source.getOriginalDocument()).thenReturn(originalDocument);

        BaseObject previousObject = mock(BaseObject.class);
        when(originalDocument.getXObjects(ApproversXClassInitializer.APPROVERS_XCLASS))
            .thenReturn(Collections.singletonList(previousObject));
        when(previousObject.getLargeStringValue(ApproversXClassInitializer.USERS_APPROVERS_PROPERTY))
            .thenReturn("user1");

        BaseObject approverObject = mock(BaseObject.class);
        when(this.source.getXObjects(ApproversXClassInitializer.APPROVERS_XCLASS))
            .thenReturn(Collections.singletonList(approverObject));
        when(approverObject.getLargeStringValue(ApproversXClassInitializer.USERS_APPROVERS_PROPERTY))
            .thenReturn("user2");

        this.listener.onEvent(this.event, this.source, null);
        verify(approverObject).apply(previousObject, true);
    }

    @Test
    void onEvent()
    {
        when(this.contextualAuthorizationManager.hasAccess(Right.ADMIN)).thenReturn(false);
        when(this.observationContext.isIn(any(ChangeRequestMergingEvent.class))).thenReturn(false);
        when(this.remoteObservationManagerContext.isRemoteState()).thenReturn(false);

        when(this.configuration.getMinimumApprovers()).thenReturn(2);
        XWikiDocument originalDocument = mock(XWikiDocument.class);
        when(this.source.getOriginalDocument()).thenReturn(originalDocument);

        BaseObject previousObject = mock(BaseObject.class);
        when(originalDocument.getXObjects(ApproversXClassInitializer.APPROVERS_XCLASS))
            .thenReturn(Collections.singletonList(previousObject));
        when(previousObject.getLargeStringValue(ApproversXClassInitializer.USERS_APPROVERS_PROPERTY))
            .thenReturn("user1");

        BaseObject approverObject = mock(BaseObject.class);
        when(this.source.getXObjects(ApproversXClassInitializer.APPROVERS_XCLASS))
            .thenReturn(Collections.singletonList(approverObject));
        when(approverObject.getLargeStringValue(ApproversXClassInitializer.USERS_APPROVERS_PROPERTY))
            .thenReturn("user2,user3");

        this.listener.onEvent(this.event, this.source, null);
        verify(approverObject, never()).apply(any(), anyBoolean());
        verify(this.source, never()).setXObject(anyInt(), any());
        verify(this.source, never()).removeXObjects(any());
    }
}
