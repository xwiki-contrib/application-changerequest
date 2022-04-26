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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ChangeRequest}.
 *
 * @version $Id$
 * @since 0.5
 */
class ChangeRequestTest
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

    @Test
    void cloneWithoutFileChanges()
    {
        UserReference userReference = mock(UserReference.class);
        ChangeRequest changeRequest = new ChangeRequest()
            .setCreator(userReference)
            .setDescription("Some description")
            .setId("4242")
            .setStatus(ChangeRequestStatus.MERGED)
            .setCreationDate(new Date(48))
            .setTitle("Another title");

        ChangeRequest otherChangeRequest = new ChangeRequest()
            .setCreator(userReference)
            .setDescription("Some description")
            .setId("4242")
            .setStatus(ChangeRequestStatus.MERGED)
            .setCreationDate(new Date(48))
            .setTitle("Another title");

        FileChange fileChange = mock(FileChange.class);
        when(fileChange.getTargetEntity()).thenReturn(mock(DocumentReference.class));
        changeRequest.addFileChange(fileChange);

        assertFalse(changeRequest.getFileChanges().isEmpty());

        ChangeRequest clone = changeRequest.cloneWithoutFileChanges();
        assertTrue(clone.getFileChanges().isEmpty());

        assertNotEquals(otherChangeRequest, clone);
        otherChangeRequest.setCreationDate(clone.getCreationDate());
        assertNotEquals(otherChangeRequest, clone);
    }

    @Test
    void getFileChangeImmediatelyBefore()
    {
        ChangeRequest changeRequest = new ChangeRequest();

        DocumentReference refA = mock(DocumentReference.class);
        DocumentReference refB = mock(DocumentReference.class);
        DocumentReference refC = mock(DocumentReference.class);

        FileChange fileChange1RefA = mock(FileChange.class);
        FileChange fileChange2RefA = mock(FileChange.class);
        FileChange fileChange3RefA = mock(FileChange.class);

        FileChange fileChange1RefB = mock(FileChange.class);
        FileChange fileChange2RefB = mock(FileChange.class);

        FileChange fileChange1RefC = mock(FileChange.class);

        when(fileChange1RefA.getTargetEntity()).thenReturn(refA);
        when(fileChange2RefA.getTargetEntity()).thenReturn(refA);
        when(fileChange3RefA.getTargetEntity()).thenReturn(refA);

        when(fileChange1RefB.getTargetEntity()).thenReturn(refB);
        when(fileChange2RefB.getTargetEntity()).thenReturn(refB);

        when(fileChange1RefC.getTargetEntity()).thenReturn(refC);

        changeRequest.addFileChange(fileChange1RefA);
        changeRequest.addFileChange(fileChange2RefA);
        changeRequest.addFileChange(fileChange3RefA);

        changeRequest.addFileChange(fileChange1RefB);
        changeRequest.addFileChange(fileChange2RefB);

        changeRequest.addFileChange(fileChange1RefC);

        assertEquals(Optional.of(fileChange2RefA), changeRequest.getFileChangeImmediatelyBefore(fileChange3RefA));
        assertEquals(Optional.of(fileChange1RefA), changeRequest.getFileChangeImmediatelyBefore(fileChange2RefA));
        assertEquals(Optional.empty(), changeRequest.getFileChangeImmediatelyBefore(fileChange1RefA));

        assertEquals(Optional.of(fileChange1RefB), changeRequest.getFileChangeImmediatelyBefore(fileChange2RefB));
        assertEquals(Optional.empty(), changeRequest.getFileChangeImmediatelyBefore(fileChange1RefB));

        assertEquals(Optional.empty(), changeRequest.getFileChangeImmediatelyBefore(fileChange1RefC));
    }

    @Test
    void getFileChangeWithChangeBefore()
    {
        ChangeRequest changeRequest = new ChangeRequest();

        DocumentReference refA = mock(DocumentReference.class);
        DocumentReference refB = mock(DocumentReference.class);
        DocumentReference refC = mock(DocumentReference.class);

        FileChange fileChange1RefA = mock(FileChange.class);
        FileChange fileChange2RefA = mock(FileChange.class);
        FileChange fileChange3RefA = mock(FileChange.class);
        FileChange fileChange4RefA = mock(FileChange.class);
        FileChange fileChange5RefA = mock(FileChange.class);
        FileChange fileChange6RefA = mock(FileChange.class);
        FileChange fileChange7RefA = mock(FileChange.class);

        FileChange fileChange1RefB = mock(FileChange.class);
        FileChange fileChange2RefB = mock(FileChange.class);
        FileChange fileChange3RefB = mock(FileChange.class);
        FileChange fileChange4RefB = mock(FileChange.class);

        FileChange fileChange1RefC = mock(FileChange.class);
        FileChange fileChange2RefC = mock(FileChange.class);
        FileChange fileChange3RefC = mock(FileChange.class);

        when(fileChange1RefA.getTargetEntity()).thenReturn(refA);
        when(fileChange2RefA.getTargetEntity()).thenReturn(refA);
        when(fileChange3RefA.getTargetEntity()).thenReturn(refA);
        when(fileChange4RefA.getTargetEntity()).thenReturn(refA);
        when(fileChange5RefA.getTargetEntity()).thenReturn(refA);
        when(fileChange6RefA.getTargetEntity()).thenReturn(refA);
        when(fileChange7RefA.getTargetEntity()).thenReturn(refA);

        when(fileChange1RefB.getTargetEntity()).thenReturn(refB);
        when(fileChange2RefB.getTargetEntity()).thenReturn(refB);
        when(fileChange3RefB.getTargetEntity()).thenReturn(refB);
        when(fileChange4RefB.getTargetEntity()).thenReturn(refB);

        when(fileChange1RefC.getTargetEntity()).thenReturn(refC);
        when(fileChange2RefC.getTargetEntity()).thenReturn(refC);
        when(fileChange3RefC.getTargetEntity()).thenReturn(refC);

        when(fileChange1RefA.getType()).thenReturn(FileChange.FileChangeType.CREATION);
        when(fileChange2RefA.getType()).thenReturn(FileChange.FileChangeType.EDITION);
        when(fileChange3RefA.getType()).thenReturn(FileChange.FileChangeType.NO_CHANGE);
        when(fileChange4RefA.getType()).thenReturn(FileChange.FileChangeType.NO_CHANGE);
        when(fileChange5RefA.getType()).thenReturn(FileChange.FileChangeType.EDITION);
        when(fileChange6RefA.getType()).thenReturn(FileChange.FileChangeType.CREATION);
        when(fileChange7RefA.getType()).thenReturn(FileChange.FileChangeType.NO_CHANGE);

        when(fileChange1RefB.getType()).thenReturn(FileChange.FileChangeType.NO_CHANGE);
        when(fileChange2RefB.getType()).thenReturn(FileChange.FileChangeType.NO_CHANGE);
        when(fileChange3RefB.getType()).thenReturn(FileChange.FileChangeType.DELETION);
        when(fileChange4RefB.getType()).thenReturn(FileChange.FileChangeType.NO_CHANGE);

        when(fileChange1RefC.getType()).thenReturn(FileChange.FileChangeType.CREATION);
        when(fileChange2RefC.getType()).thenReturn(FileChange.FileChangeType.NO_CHANGE);
        when(fileChange3RefC.getType()).thenReturn(FileChange.FileChangeType.NO_CHANGE);

        changeRequest.addFileChange(fileChange1RefA);
        changeRequest.addFileChange(fileChange2RefA);
        changeRequest.addFileChange(fileChange3RefA);
        changeRequest.addFileChange(fileChange4RefA);
        changeRequest.addFileChange(fileChange5RefA);
        changeRequest.addFileChange(fileChange6RefA);
        changeRequest.addFileChange(fileChange7RefA);

        changeRequest.addFileChange(fileChange1RefB);
        changeRequest.addFileChange(fileChange2RefB);
        changeRequest.addFileChange(fileChange3RefB);
        changeRequest.addFileChange(fileChange4RefB);

        changeRequest.addFileChange(fileChange1RefC);
        changeRequest.addFileChange(fileChange2RefC);
        changeRequest.addFileChange(fileChange3RefC);

        assertEquals(Optional.of(fileChange2RefA), changeRequest.getFileChangeWithChangeBefore(fileChange3RefA));
        assertEquals(Optional.of(fileChange2RefA), changeRequest.getFileChangeWithChangeBefore(fileChange2RefA));
        assertEquals(Optional.of(fileChange1RefA), changeRequest.getFileChangeWithChangeBefore(fileChange1RefA));
        assertEquals(Optional.of(fileChange6RefA), changeRequest.getFileChangeWithChangeBefore(fileChange7RefA));
        assertEquals(Optional.of(fileChange2RefA), changeRequest.getFileChangeWithChangeBefore(fileChange4RefA));

        assertEquals(Optional.of(fileChange3RefB), changeRequest.getFileChangeWithChangeBefore(fileChange4RefB));
        assertEquals(Optional.of(fileChange3RefB), changeRequest.getFileChangeWithChangeBefore(fileChange3RefB));
        assertEquals(Optional.empty(), changeRequest.getFileChangeWithChangeBefore(fileChange2RefB));
        assertEquals(Optional.empty(), changeRequest.getFileChangeWithChangeBefore(fileChange1RefB));

        assertEquals(Optional.of(fileChange1RefC), changeRequest.getFileChangeWithChangeBefore(fileChange3RefC));
    }

    @Test
    void getLatestReviewFrom()
    {
        ChangeRequest changeRequest = new ChangeRequest();
        UserReference userReference1 = mock(UserReference.class);
        UserReference userReference2 = mock(UserReference.class);
        UserReference userReference3 = mock(UserReference.class);

        ChangeRequestReview review1 = mock(ChangeRequestReview.class);
        ChangeRequestReview review2 = mock(ChangeRequestReview.class);
        ChangeRequestReview review3 = mock(ChangeRequestReview.class);
        ChangeRequestReview review4 = mock(ChangeRequestReview.class);
        ChangeRequestReview review5 = mock(ChangeRequestReview.class);

        when(review1.getAuthor()).thenReturn(userReference1);
        when(review2.getAuthor()).thenReturn(userReference2);
        when(review3.getAuthor()).thenReturn(userReference2);
        when(review4.getAuthor()).thenReturn(userReference3);
        when(review5.getAuthor()).thenReturn(userReference1);

        changeRequest.addReview(review1)
            .addReview(review2)
            .addReview(review3)
            .addReview(review4)
            .addReview(review5);

        assertEquals(Optional.of(review5), changeRequest.getLatestReviewFrom(userReference1));
        assertEquals(Optional.of(review4), changeRequest.getLatestReviewFrom(userReference3));
        assertEquals(Optional.of(review3), changeRequest.getLatestReviewFrom(userReference2));
        assertEquals(Optional.empty(), changeRequest.getLatestReviewFrom(mock(UserReference.class)));
    }

    @Test
    void getLatestReviewFromOrOnBehalfOf()
    {
        ChangeRequest changeRequest = new ChangeRequest();
        UserReference userReference1 = mock(UserReference.class);
        UserReference userReference2 = mock(UserReference.class);
        UserReference userReference3 = mock(UserReference.class);
        UserReference userReference4 = mock(UserReference.class);
        UserReference userReference5 = mock(UserReference.class);

        ChangeRequestReview review1 = mock(ChangeRequestReview.class);
        ChangeRequestReview review2 = mock(ChangeRequestReview.class);
        ChangeRequestReview review3 = mock(ChangeRequestReview.class);
        ChangeRequestReview review4 = mock(ChangeRequestReview.class);
        ChangeRequestReview review5 = mock(ChangeRequestReview.class);
        ChangeRequestReview review6 = mock(ChangeRequestReview.class);

        when(review1.getAuthor()).thenReturn(userReference5);
        when(review2.getAuthor()).thenReturn(userReference2);

        when(review3.getAuthor()).thenReturn(userReference2);
        when(review3.getOriginalApprover()).thenReturn(userReference4);

        when(review4.getAuthor()).thenReturn(userReference3);
        when(review4.getOriginalApprover()).thenReturn(userReference1);

        when(review5.getAuthor()).thenReturn(userReference1);
        when(review5.getOriginalApprover()).thenReturn(userReference3);

        when(review6.getAuthor()).thenReturn(userReference5);
        when(review6.getOriginalApprover()).thenReturn(userReference5);

        changeRequest.addReview(review1)
            .addReview(review2)
            .addReview(review3)
            .addReview(review4)
            .addReview(review5)
            .addReview(review6);

        assertEquals(Optional.of(review4), changeRequest.getLatestReviewFromOrOnBehalfOf(userReference1));
        assertEquals(Optional.of(review2), changeRequest.getLatestReviewFromOrOnBehalfOf(userReference2));
        assertEquals(Optional.of(review5), changeRequest.getLatestReviewFromOrOnBehalfOf(userReference3));
        assertEquals(Optional.of(review3), changeRequest.getLatestReviewFromOrOnBehalfOf(userReference4));
        assertEquals(Optional.of(review6), changeRequest.getLatestReviewFromOrOnBehalfOf(userReference5));
        assertEquals(Optional.empty(), changeRequest.getLatestReviewFromOrOnBehalfOf(mock(UserReference.class)));
    }
}
