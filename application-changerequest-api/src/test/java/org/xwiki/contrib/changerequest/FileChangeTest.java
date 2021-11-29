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

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.user.UserReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link FileChange}.
 *
 * @version $Id$
 * @since 0.7
 */
class FileChangeTest
{
    @Test
    void equals()
    {
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        UserReference user1 = mock(UserReference.class);
        DocumentReference targetEntity = mock(DocumentReference.class);
        FileChange fileChange = new FileChange(changeRequest, FileChange.FileChangeType.EDITION)
            .setVersion("2.1-filechange")
            .setAuthor(user1)
            .setId("someId")
            .setPreviousVersion("1.1-filechange")
            .setTargetEntity(targetEntity)
            .setPreviousPublishedVersion("1.1", new Date(42))
            .setCreationDate(new Date(21));

        FileChange otherFileChange = new FileChange(changeRequest, FileChange.FileChangeType.EDITION)
            .setVersion("2.1-filechange")
            .setAuthor(user1)
            .setId("someId")
            .setPreviousVersion("1.1-filechange")
            .setTargetEntity(targetEntity)
            .setPreviousPublishedVersion("1.1", new Date(42))
            .setCreationDate(new Date(21));

        assertEquals(fileChange, otherFileChange);

        otherFileChange = new FileChange(changeRequest, FileChange.FileChangeType.DELETION)
            .setVersion("2.1-filechange")
            .setAuthor(user1)
            .setId("someId")
            .setPreviousVersion("1.1-filechange")
            .setTargetEntity(targetEntity)
            .setPreviousPublishedVersion("1.1", new Date(42))
            .setCreationDate(new Date(21));

        assertNotEquals(fileChange, otherFileChange);

        // we don't consider change request in equals to avoid any stackoverflow
        otherFileChange = new FileChange(mock(ChangeRequest.class), FileChange.FileChangeType.EDITION)
            .setVersion("2.1-filechange")
            .setAuthor(user1)
            .setId("someId")
            .setPreviousVersion("1.1-filechange")
            .setTargetEntity(targetEntity)
            .setPreviousPublishedVersion("1.1", new Date(42))
            .setCreationDate(new Date(21));

        assertEquals(fileChange, otherFileChange);

        otherFileChange = new FileChange(changeRequest, FileChange.FileChangeType.EDITION)
            .setVersion("2.1")
            .setAuthor(user1)
            .setId("someId")
            .setPreviousVersion("1.1-filechange")
            .setTargetEntity(targetEntity)
            .setPreviousPublishedVersion("1.1", new Date(42))
            .setCreationDate(new Date(21));

        assertNotEquals(fileChange, otherFileChange);

        otherFileChange = new FileChange(changeRequest, FileChange.FileChangeType.EDITION)
            .setVersion("2.1-filechange")
            .setAuthor(mock(UserReference.class))
            .setId("someId")
            .setPreviousVersion("1.1-filechange")
            .setTargetEntity(targetEntity)
            .setPreviousPublishedVersion("1.1", new Date(42))
            .setCreationDate(new Date(21));

        assertNotEquals(fileChange, otherFileChange);

        otherFileChange = new FileChange(changeRequest, FileChange.FileChangeType.EDITION)
            .setVersion("2.1-filechange")
            .setAuthor(user1)
            .setId("otherId")
            .setPreviousVersion("1.1-filechange")
            .setTargetEntity(targetEntity)
            .setPreviousPublishedVersion("1.1", new Date(42))
            .setCreationDate(new Date(21));

        assertNotEquals(fileChange, otherFileChange);

        otherFileChange = new FileChange(changeRequest, FileChange.FileChangeType.EDITION)
            .setVersion("2.1-filechange")
            .setAuthor(user1)
            .setId("someId")
            .setPreviousVersion("1.1")
            .setTargetEntity(targetEntity)
            .setPreviousPublishedVersion("1.1", new Date(42))
            .setCreationDate(new Date(21));

        assertNotEquals(fileChange, otherFileChange);

        otherFileChange = new FileChange(changeRequest, FileChange.FileChangeType.EDITION)
            .setVersion("2.1-filechange")
            .setAuthor(user1)
            .setId("someId")
            .setPreviousVersion("1.1-filechange")
            .setTargetEntity(mock(DocumentReference.class))
            .setPreviousPublishedVersion("1.1", new Date(42))
            .setCreationDate(new Date(21));

        assertNotEquals(fileChange, otherFileChange);

        otherFileChange = new FileChange(changeRequest, FileChange.FileChangeType.EDITION)
            .setVersion("2.1-filechange")
            .setAuthor(user1)
            .setId("someId")
            .setPreviousVersion("1.1-filechange")
            .setTargetEntity(targetEntity)
            .setPreviousPublishedVersion("1.1-filechange", new Date(42))
            .setCreationDate(new Date(21));

        assertNotEquals(fileChange, otherFileChange);

        otherFileChange = new FileChange(changeRequest, FileChange.FileChangeType.EDITION)
            .setVersion("2.1-filechange")
            .setAuthor(user1)
            .setId("someId")
            .setPreviousVersion("1.1-filechange")
            .setTargetEntity(targetEntity)
            .setPreviousPublishedVersion("1.1", new Date(43))
            .setCreationDate(new Date(21));

        assertNotEquals(fileChange, otherFileChange);

        otherFileChange = new FileChange(changeRequest, FileChange.FileChangeType.EDITION)
            .setVersion("2.1-filechange")
            .setAuthor(user1)
            .setId("someId")
            .setPreviousVersion("1.1-filechange")
            .setTargetEntity(targetEntity)
            .setPreviousPublishedVersion("1.1", new Date(42))
            .setCreationDate(new Date(22));

        assertNotEquals(fileChange, otherFileChange);
    }

    @Test
    void cloneWithChangeRequest()
    {
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        UserReference user1 = mock(UserReference.class);
        DocumentReference targetEntity = mock(DocumentReference.class);
        FileChange fileChange = new FileChange(changeRequest, FileChange.FileChangeType.EDITION)
            .setVersion("2.1-filechange")
            .setAuthor(user1)
            .setId("someId")
            .setPreviousVersion("1.1-filechange")
            .setTargetEntity(targetEntity)
            .setPreviousPublishedVersion("1.1", new Date(42))
            .setCreationDate(new Date(21));

        ChangeRequest otherChangeRequest = mock(ChangeRequest.class);
        FileChange fileChange1 = fileChange.cloneWithChangeRequest(otherChangeRequest);
        assertEquals(fileChange, fileChange1);

        assertEquals(otherChangeRequest, fileChange1.getChangeRequest());
    }
}
