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
package org.xwiki.contrib.changerequest;

import java.util.Arrays;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.user.UserReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ChangeRequest}.
 *
 * @version $Id$
 * @since 0.5
 */
public class ChangeRequestTest
{
    @Test
    void addFileChange()
    {
        ChangeRequest changeRequest = new ChangeRequest();
        DocumentReference ref1 = mock(DocumentReference.class);
        DocumentReference ref2 = mock(DocumentReference.class);
        DocumentReference ref3 = mock(DocumentReference.class);

        FileChange fileChange1Ref1 = mock(FileChange.class);
        FileChange fileChange2Ref1 = mock(FileChange.class);
        FileChange fileChange3Ref1 = mock(FileChange.class);
        FileChange fileChange1Ref2 = mock(FileChange.class);
        FileChange fileChange1Ref3 = mock(FileChange.class);
        FileChange fileChange2Ref3 = mock(FileChange.class);

        UserReference author1 = mock(UserReference.class);
        UserReference author2 = mock(UserReference.class);
        UserReference author3 = mock(UserReference.class);
        UserReference author4 = mock(UserReference.class);

        when(fileChange1Ref1.getTargetEntity()).thenReturn(ref1);
        when(fileChange2Ref1.getTargetEntity()).thenReturn(ref1);
        when(fileChange3Ref1.getTargetEntity()).thenReturn(ref1);

        when(fileChange1Ref2.getTargetEntity()).thenReturn(ref2);

        when(fileChange1Ref3.getTargetEntity()).thenReturn(ref3);
        when(fileChange2Ref3.getTargetEntity()).thenReturn(ref3);

        when(fileChange1Ref1.getAuthor()).thenReturn(author1);
        when(fileChange2Ref1.getAuthor()).thenReturn(author1);
        when(fileChange3Ref1.getAuthor()).thenReturn(author2);

        when(fileChange1Ref2.getAuthor()).thenReturn(author3);

        when(fileChange1Ref3.getAuthor()).thenReturn(author1);
        when(fileChange2Ref3.getAuthor()).thenReturn(author4);

        assertSame(changeRequest, changeRequest.addFileChange(fileChange1Ref1));
        assertSame(changeRequest, changeRequest.addFileChange(fileChange2Ref1));
        assertSame(changeRequest, changeRequest.addFileChange(fileChange3Ref1));

        assertSame(changeRequest, changeRequest.addFileChange(fileChange1Ref2));

        assertSame(changeRequest, changeRequest.addFileChange(fileChange1Ref3));
        assertSame(changeRequest, changeRequest.addFileChange(fileChange2Ref3));

        Map<DocumentReference, Deque<FileChange>> expectedMap = new HashMap<>();
        LinkedList<FileChange> dequeFileChange = new LinkedList<>();
        dequeFileChange.add(fileChange1Ref1);
        dequeFileChange.add(fileChange2Ref1);
        dequeFileChange.add(fileChange3Ref1);
        expectedMap.put(ref1, dequeFileChange);

        dequeFileChange = new LinkedList<>();
        dequeFileChange.add(fileChange1Ref2);
        expectedMap.put(ref2, dequeFileChange);

        dequeFileChange = new LinkedList<>();
        dequeFileChange.add(fileChange1Ref3);
        dequeFileChange.add(fileChange2Ref3);
        expectedMap.put(ref3, dequeFileChange);

        assertEquals(expectedMap, changeRequest.getFileChanges());
        assertEquals(new HashSet<>(Arrays.asList(author1, author2, author3, author4)), changeRequest.getAuthors());
    }

    @Test
    void getLastFileChanges()
    {
        ChangeRequest changeRequest = new ChangeRequest();
        DocumentReference ref1 = mock(DocumentReference.class);
        DocumentReference ref2 = mock(DocumentReference.class);
        DocumentReference ref3 = mock(DocumentReference.class);

        FileChange fileChange1Ref1 = mock(FileChange.class);
        FileChange fileChange2Ref1 = mock(FileChange.class);
        FileChange fileChange3Ref1 = mock(FileChange.class);
        FileChange fileChange1Ref2 = mock(FileChange.class);
        FileChange fileChange1Ref3 = mock(FileChange.class);
        FileChange fileChange2Ref3 = mock(FileChange.class);

        when(fileChange1Ref1.getTargetEntity()).thenReturn(ref1);
        when(fileChange2Ref1.getTargetEntity()).thenReturn(ref1);
        when(fileChange3Ref1.getTargetEntity()).thenReturn(ref1);

        when(fileChange1Ref2.getTargetEntity()).thenReturn(ref2);

        when(fileChange1Ref3.getTargetEntity()).thenReturn(ref3);
        when(fileChange2Ref3.getTargetEntity()).thenReturn(ref3);

        changeRequest.addFileChange(fileChange1Ref1);
        changeRequest.addFileChange(fileChange2Ref1);
        changeRequest.addFileChange(fileChange3Ref1);

        changeRequest.addFileChange(fileChange1Ref2);

        changeRequest.addFileChange(fileChange1Ref3);
        changeRequest.addFileChange(fileChange2Ref3);

        assertEquals(Arrays.asList(fileChange3Ref1, fileChange1Ref2, fileChange2Ref3),
            changeRequest.getLastFileChanges());
    }

    @Test
    void getAllFileChanges()
    {
        ChangeRequest changeRequest = new ChangeRequest();
        DocumentReference ref1 = mock(DocumentReference.class);
        DocumentReference ref2 = mock(DocumentReference.class);
        DocumentReference ref3 = mock(DocumentReference.class);

        FileChange fileChange1Ref1 = mock(FileChange.class);
        FileChange fileChange2Ref1 = mock(FileChange.class);
        FileChange fileChange3Ref1 = mock(FileChange.class);
        FileChange fileChange1Ref2 = mock(FileChange.class);
        FileChange fileChange1Ref3 = mock(FileChange.class);
        FileChange fileChange2Ref3 = mock(FileChange.class);

        when(fileChange1Ref1.getTargetEntity()).thenReturn(ref1);
        when(fileChange2Ref1.getTargetEntity()).thenReturn(ref1);
        when(fileChange3Ref1.getTargetEntity()).thenReturn(ref1);

        when(fileChange1Ref2.getTargetEntity()).thenReturn(ref2);

        when(fileChange1Ref3.getTargetEntity()).thenReturn(ref3);
        when(fileChange2Ref3.getTargetEntity()).thenReturn(ref3);

        changeRequest.addFileChange(fileChange1Ref1);
        changeRequest.addFileChange(fileChange2Ref1);
        changeRequest.addFileChange(fileChange3Ref1);

        changeRequest.addFileChange(fileChange1Ref2);

        changeRequest.addFileChange(fileChange1Ref3);
        changeRequest.addFileChange(fileChange2Ref3);

        assertEquals(Arrays.asList(
                fileChange1Ref1,
                fileChange2Ref1,
                fileChange3Ref1,
                fileChange1Ref2,
                fileChange1Ref3,
                fileChange2Ref3),
            changeRequest.getAllFileChanges());
    }

    @Test
    void getLatestFileChangeFor()
    {
        ChangeRequest changeRequest = new ChangeRequest();
        DocumentReference ref1 = mock(DocumentReference.class);
        DocumentReference ref2 = mock(DocumentReference.class);
        DocumentReference ref3 = mock(DocumentReference.class);

        FileChange fileChange1Ref1 = mock(FileChange.class);
        FileChange fileChange2Ref1 = mock(FileChange.class);
        FileChange fileChange3Ref1 = mock(FileChange.class);
        FileChange fileChange1Ref2 = mock(FileChange.class);
        FileChange fileChange1Ref3 = mock(FileChange.class);
        FileChange fileChange2Ref3 = mock(FileChange.class);

        when(fileChange1Ref1.getTargetEntity()).thenReturn(ref1);
        when(fileChange2Ref1.getTargetEntity()).thenReturn(ref1);
        when(fileChange3Ref1.getTargetEntity()).thenReturn(ref1);

        when(fileChange1Ref2.getTargetEntity()).thenReturn(ref2);

        when(fileChange1Ref3.getTargetEntity()).thenReturn(ref3);
        when(fileChange2Ref3.getTargetEntity()).thenReturn(ref3);

        changeRequest.addFileChange(fileChange1Ref1);
        changeRequest.addFileChange(fileChange2Ref1);
        changeRequest.addFileChange(fileChange3Ref1);

        changeRequest.addFileChange(fileChange1Ref2);

        changeRequest.addFileChange(fileChange1Ref3);
        changeRequest.addFileChange(fileChange2Ref3);

        assertEquals(Optional.of(fileChange3Ref1), changeRequest.getLatestFileChangeFor(ref1));
        assertEquals(Optional.empty(), changeRequest.getLatestFileChangeFor(mock(DocumentReference.class)));
        assertEquals(Optional.of(fileChange1Ref2), changeRequest.getLatestFileChangeFor(ref2));
        assertEquals(Optional.of(fileChange2Ref3), changeRequest.getLatestFileChangeFor(ref3));
    }

    @Test
    void equals()
    {
        UserReference userReference = mock(UserReference.class);
        ChangeRequest changeRequest = new ChangeRequest()
            .setCreator(userReference)
            .setDescription("Some description")
            .setId("4242")
            .setStatus(ChangeRequestStatus.MERGED)
            .setCreationDate(new Date(48))
            .setTitle("A title");

        ChangeRequest otherChangeRequest = new ChangeRequest()
            .setCreator(userReference)
            .setDescription("Some description")
            .setId("4242")
            .setStatus(ChangeRequestStatus.MERGED)
            .setCreationDate(new Date(48))
            .setTitle("A title");
        assertEquals(changeRequest, otherChangeRequest);

        otherChangeRequest = new ChangeRequest()
            .setCreator(mock(UserReference.class))
            .setDescription("Some description")
            .setId("4242")
            .setStatus(ChangeRequestStatus.MERGED)
            .setCreationDate(new Date(48))
            .setTitle("A title");
        assertNotEquals(changeRequest, otherChangeRequest);

        otherChangeRequest = new ChangeRequest()
            .setCreator(userReference)
            .setDescription("Some other description")
            .setId("4242")
            .setStatus(ChangeRequestStatus.MERGED)
            .setCreationDate(new Date(48))
            .setTitle("A title");
        assertNotEquals(changeRequest, otherChangeRequest);

        otherChangeRequest = new ChangeRequest()
            .setCreator(userReference)
            .setDescription("Some description")
            .setId("4342")
            .setStatus(ChangeRequestStatus.MERGED)
            .setCreationDate(new Date(48))
            .setTitle("A title");
        assertNotEquals(changeRequest, otherChangeRequest);

        otherChangeRequest = new ChangeRequest()
            .setCreator(userReference)
            .setDescription("Some description")
            .setId("4242")
            .setStatus(ChangeRequestStatus.DRAFT)
            .setCreationDate(new Date(48))
            .setTitle("A title");
        assertNotEquals(changeRequest, otherChangeRequest);

        otherChangeRequest = new ChangeRequest()
            .setCreator(userReference)
            .setDescription("Some description")
            .setId("4242")
            .setStatus(ChangeRequestStatus.MERGED)
            .setCreationDate(new Date(49))
            .setTitle("A title");
        assertNotEquals(changeRequest, otherChangeRequest);

        otherChangeRequest = new ChangeRequest()
            .setCreator(userReference)
            .setDescription("Some description")
            .setId("4242")
            .setStatus(ChangeRequestStatus.MERGED)
            .setCreationDate(new Date(48))
            .setTitle("Another title");
        assertNotEquals(changeRequest, otherChangeRequest);
    }
}
