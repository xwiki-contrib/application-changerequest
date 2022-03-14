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
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.contrib.changerequest.ApproversManager;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link FileChangeApproversManager}.
 *
 * @version $Id$
 * @since 0.11
 */
@ComponentTest
class FileChangeApproversManagerTest
{
    @InjectMockComponents
    private FileChangeApproversManager approversManager;

    @MockComponent
    private ApproversManager<XWikiDocument> documentApproversManager;

    @MockComponent
    private ApproversManager<DocumentReference> documentReferenceApproversManager;

    @Mock
    private FileChange fileChange;

    @Mock
    private XWikiDocument xWikiDocument;

    @Mock
    private DocumentReference documentReference;

    @BeforeEach
    void setup()
    {
        when(fileChange.getModifiedDocument()).thenReturn(this.xWikiDocument);
        when(fileChange.getTargetEntity()).thenReturn(this.documentReference);
    }

    @Test
    void isApprover() throws ChangeRequestException
    {
        UserReference userReference = mock(UserReference.class);
        when(fileChange.getType()).thenReturn(FileChange.FileChangeType.CREATION);
        when(this.documentApproversManager.isApprover(userReference, this.xWikiDocument, true)).thenReturn(true);
        assertTrue(this.approversManager.isApprover(userReference, this.fileChange, true));
        verify(this.documentApproversManager).isApprover(userReference, this.xWikiDocument, true);

        when(this.documentReferenceApproversManager.isApprover(userReference, this.documentReference, true))
            .thenReturn(true);
        when(fileChange.getType()).thenReturn(FileChange.FileChangeType.DELETION);
        assertTrue(this.approversManager.isApprover(userReference, this.fileChange, true));

        when(fileChange.getType()).thenReturn(FileChange.FileChangeType.EDITION);
        assertTrue(this.approversManager.isApprover(userReference, this.fileChange, true));

        when(fileChange.getType()).thenReturn(FileChange.FileChangeType.NO_CHANGE);
        assertTrue(this.approversManager.isApprover(userReference, this.fileChange, true));

        verify(this.documentReferenceApproversManager, times(3))
            .isApprover(userReference, this.documentReference, true);
    }

    @Test
    void getAllApprovers() throws ChangeRequestException
    {
        UserReference userReference1 = mock(UserReference.class);
        UserReference userReference2 = mock(UserReference.class);

        Set<UserReference> expectedSet = new HashSet<>(Arrays.asList(userReference1, userReference2));
        when(this.documentApproversManager.getAllApprovers(this.xWikiDocument, true)).thenReturn(expectedSet);
        when(fileChange.getType()).thenReturn(FileChange.FileChangeType.CREATION);

        assertSame(expectedSet, this.approversManager.getAllApprovers(this.fileChange, true));
        verify(this.documentApproversManager).getAllApprovers(this.xWikiDocument, true);

        when(this.documentReferenceApproversManager.getAllApprovers(this.documentReference, true))
            .thenReturn(expectedSet);
        when(fileChange.getType()).thenReturn(FileChange.FileChangeType.DELETION);
        assertSame(expectedSet, this.approversManager.getAllApprovers(this.fileChange, true));

        when(fileChange.getType()).thenReturn(FileChange.FileChangeType.EDITION);
        assertSame(expectedSet, this.approversManager.getAllApprovers(this.fileChange, true));

        when(fileChange.getType()).thenReturn(FileChange.FileChangeType.NO_CHANGE);
        assertSame(expectedSet, this.approversManager.getAllApprovers(this.fileChange, true));

        verify(this.documentReferenceApproversManager, times(3))
            .getAllApprovers(this.documentReference, true);
    }

    @Test
    void getGroupsApprovers() throws ChangeRequestException
    {
        DocumentReference documentReference1 = mock(DocumentReference.class);
        DocumentReference documentReference2 = mock(DocumentReference.class);

        Set<DocumentReference> expectedSet = new HashSet<>(Arrays.asList(documentReference1, documentReference2));
        when(this.documentApproversManager.getGroupsApprovers(this.xWikiDocument)).thenReturn(expectedSet);
        when(fileChange.getType()).thenReturn(FileChange.FileChangeType.CREATION);

        assertSame(expectedSet, this.approversManager.getGroupsApprovers(this.fileChange));
        verify(this.documentApproversManager).getGroupsApprovers(this.xWikiDocument);

        when(this.documentReferenceApproversManager.getGroupsApprovers(this.documentReference))
            .thenReturn(expectedSet);
        when(fileChange.getType()).thenReturn(FileChange.FileChangeType.DELETION);
        assertSame(expectedSet, this.approversManager.getGroupsApprovers(this.fileChange));

        when(fileChange.getType()).thenReturn(FileChange.FileChangeType.EDITION);
        assertSame(expectedSet, this.approversManager.getGroupsApprovers(this.fileChange));

        when(fileChange.getType()).thenReturn(FileChange.FileChangeType.NO_CHANGE);
        assertSame(expectedSet, this.approversManager.getGroupsApprovers(this.fileChange));

        verify(this.documentReferenceApproversManager, times(3)).getGroupsApprovers(this.documentReference);
    }

    @Test
    void setUsersApprovers()
    {
        ChangeRequestException changeRequestException = assertThrows(ChangeRequestException.class, () -> {
            this.approversManager.setUsersApprovers(Collections.emptySet(), this.fileChange);
        });
        assertEquals("Unsupported method: setters are not supported in the FileChangeApproversManager",
            changeRequestException.getMessage());
    }

    @Test
    void setGroupsApprovers()
    {
        ChangeRequestException changeRequestException = assertThrows(ChangeRequestException.class, () -> {
            this.approversManager.setGroupsApprovers(Collections.emptySet(), this.fileChange);
        });
        assertEquals("Unsupported method: setters are not supported in the FileChangeApproversManager",
            changeRequestException.getMessage());
    }
}
