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
package org.xwiki.contrib.changerequest.internal.approvers;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.xwiki.contrib.changerequest.ApproversManager;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ChangeRequestApproversManager}.
 *
 * @version $Id$
 * @since 0.5
 */
@ComponentTest
class ChangeRequestApproversManagerTest
{
    @InjectMockComponents
    private ChangeRequestApproversManager manager;

    @MockComponent
    private DocumentReferenceResolver<ChangeRequest> changeRequestDocumentReferenceResolver;

    @MockComponent
    private ApproversManager<DocumentReference> documentReferenceApproversManager;

    @Test
    void getAllApprovers() throws ChangeRequestException
    {
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        DocumentReference documentReference = mock(DocumentReference.class);

        when(this.changeRequestDocumentReferenceResolver.resolve(changeRequest)).thenReturn(documentReference);
        Set<UserReference> expectedSet1 = mock(Set.class);
        Set<UserReference> expectedSet2 = mock(Set.class);

        when(this.documentReferenceApproversManager.getAllApprovers(documentReference, false)).thenReturn(expectedSet1);
        when(this.documentReferenceApproversManager.getAllApprovers(documentReference, true)).thenReturn(expectedSet2);
        assertSame(expectedSet1, this.manager.getAllApprovers(changeRequest, false));
        assertSame(expectedSet2, this.manager.getAllApprovers(changeRequest, true));
    }

    @Test
    void setUsersApprovers() throws ChangeRequestException
    {
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        DocumentReference documentReference = mock(DocumentReference.class);

        when(this.changeRequestDocumentReferenceResolver.resolve(changeRequest)).thenReturn(documentReference);
        Set<UserReference> userReferenceSet = mock(Set.class);
        this.manager.setUsersApprovers(userReferenceSet, changeRequest);

        verify(this.documentReferenceApproversManager).setUsersApprovers(userReferenceSet, documentReference);
    }

    @Test
    void setGroupsApprovers() throws ChangeRequestException
    {
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        DocumentReference documentReference = mock(DocumentReference.class);

        when(this.changeRequestDocumentReferenceResolver.resolve(changeRequest)).thenReturn(documentReference);
        Set<DocumentReference> groupReferenceSet = mock(Set.class);
        this.manager.setGroupsApprovers(groupReferenceSet, changeRequest);

        verify(this.documentReferenceApproversManager).setGroupsApprovers(groupReferenceSet, documentReference);
    }

    @Test
    void isApprover() throws Exception
    {
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        DocumentReference documentReference = mock(DocumentReference.class);

        when(this.changeRequestDocumentReferenceResolver.resolve(changeRequest)).thenReturn(documentReference);
        UserReference user = mock(UserReference.class);

        when(this.documentReferenceApproversManager.isApprover(user, documentReference, true)).thenReturn(true);
        when(this.documentReferenceApproversManager.isApprover(user, documentReference, false)).thenReturn(true);

        assertTrue(this.manager.isApprover(user, changeRequest, true));
        assertTrue(this.manager.isApprover(user, changeRequest, false));

        when(this.documentReferenceApproversManager.isApprover(user, documentReference, true)).thenReturn(false);
        when(this.documentReferenceApproversManager.isApprover(user, documentReference, false)).thenReturn(false);

        assertFalse(this.manager.isApprover(user, changeRequest, true));
        verify(this.documentReferenceApproversManager, never()).getAllApprovers(documentReference, true);

        when(this.documentReferenceApproversManager.getAllApprovers(documentReference, true))
            .thenReturn(Collections.singleton(mock(UserReference.class)));
        assertFalse(this.manager.isApprover(user, changeRequest, false));

        when(this.documentReferenceApproversManager.getAllApprovers(documentReference, true))
            .thenReturn(Collections.emptySet());
        DocumentReference reference1 = mock(DocumentReference.class);
        DocumentReference reference2 = mock(DocumentReference.class);
        when(changeRequest.getModifiedDocuments()).thenReturn(new HashSet<>(Arrays.asList(reference1, reference2)));

        when(this.documentReferenceApproversManager.isApprover(user, reference1, false)).thenReturn(true);
        when(this.documentReferenceApproversManager.isApprover(user, reference2, false)).thenReturn(false);
        assertFalse(this.manager.isApprover(user, changeRequest, false));

        when(this.documentReferenceApproversManager.isApprover(user, reference2, false)).thenReturn(true);
        assertTrue(this.manager.isApprover(user, changeRequest, false));
        assertFalse(this.manager.isApprover(user, changeRequest, true));
    }
}
