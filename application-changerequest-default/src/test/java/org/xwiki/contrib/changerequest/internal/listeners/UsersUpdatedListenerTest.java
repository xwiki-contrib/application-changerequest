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

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.contrib.changerequest.ChangeRequestConfiguration;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.DelegateApproverManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.remote.RemoteObservationManagerContext;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link UsersUpdatedListener}
 *
 * @version $Id$
 * @since 0.13
 */
@ComponentTest
class UsersUpdatedListenerTest
{
    @InjectMockComponents
    private UsersUpdatedListener usersUpdatedListener;

    @MockComponent
    private Provider<DelegateApproverManager<XWikiDocument>> delegateApproverManagerProvider;

    @MockComponent
    private ChangeRequestConfiguration configuration;

    @MockComponent
    @Named("document")
    private UserReferenceResolver<DocumentReference> userReferenceResolver;

    @MockComponent
    private RemoteObservationManagerContext remoteObservationManagerContext;

    private DelegateApproverManager<XWikiDocument> delegateApproverManager;

    @BeforeEach
    void setup()
    {
        this.delegateApproverManager = mock(DelegateApproverManager.class);
        when(this.delegateApproverManagerProvider.get()).thenReturn(this.delegateApproverManager);
    }

    @Test
    void onEvent() throws ChangeRequestException
    {
        XWikiDocument source = mock(XWikiDocument.class);
        when(this.configuration.isDelegateEnabled()).thenReturn(false);
        when(this.configuration.getDelegateClassPropertyList()).thenReturn(Collections.emptyList());
        when(this.remoteObservationManagerContext.isRemoteState()).thenReturn(true);

        this.usersUpdatedListener.onEvent(null, source, null);
        verifyNoInteractions(this.delegateApproverManager);

        when(this.remoteObservationManagerContext.isRemoteState()).thenReturn(false);
        this.usersUpdatedListener.onEvent(null, source, null);
        verifyNoInteractions(this.delegateApproverManager);

        when(this.configuration.getDelegateClassPropertyList()).thenReturn(Collections.singletonList("delegate"));
        this.usersUpdatedListener.onEvent(null, source, null);
        verifyNoInteractions(this.delegateApproverManager);

        when(this.configuration.isDelegateEnabled()).thenReturn(true);
        when(this.remoteObservationManagerContext.isRemoteState()).thenReturn(true);
        this.usersUpdatedListener.onEvent(null, source, null);
        verifyNoInteractions(this.delegateApproverManager);

        when(this.remoteObservationManagerContext.isRemoteState()).thenReturn(false);
        when(this.configuration.isDelegateEnabled()).thenReturn(true);
        when(this.configuration.getDelegateClassPropertyList()).thenReturn(Collections.emptyList());
        this.usersUpdatedListener.onEvent(null, source, null);
        verifyNoInteractions(this.delegateApproverManager);

        when(this.configuration.getDelegateClassPropertyList()).thenReturn(Collections.singletonList("delegate"));
        DocumentReference documentReference = mock(DocumentReference.class);
        when(source.getDocumentReference()).thenReturn(documentReference);
        UserReference userReference = mock(UserReference.class);
        when(this.userReferenceResolver.resolve(documentReference)).thenReturn(userReference);
        this.usersUpdatedListener.onEvent(null, source, null);

        verify(this.delegateApproverManager).computeDelegates(userReference);
    }
}
