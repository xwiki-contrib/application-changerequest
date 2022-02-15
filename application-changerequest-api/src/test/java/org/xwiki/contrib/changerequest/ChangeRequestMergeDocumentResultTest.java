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
import org.xwiki.store.merge.MergeDocumentResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ChangeRequestMergeDocumentResult}.
 *
 * @version $Id$
 * @since 0.5
 */
class ChangeRequestMergeDocumentResultTest
{
    @Test
    void hasConflicts()
    {
        MergeDocumentResult mergeDocumentResult = mock(MergeDocumentResult.class);
        when(mergeDocumentResult.hasConflicts()).thenReturn(false);

        FileChange fileChange = mock(FileChange.class);
        when(fileChange.getType()).thenReturn(FileChange.FileChangeType.EDITION);
        ChangeRequestMergeDocumentResult changeRequestMergeDocumentResult =
            new ChangeRequestMergeDocumentResult(mergeDocumentResult, fileChange, "1.3", new Date(45));
        assertFalse(changeRequestMergeDocumentResult.hasConflicts());

        when(mergeDocumentResult.hasConflicts()).thenReturn(true);
        changeRequestMergeDocumentResult =
            new ChangeRequestMergeDocumentResult(mergeDocumentResult, fileChange, "1.3", new Date(45));
        assertTrue(changeRequestMergeDocumentResult.hasConflicts());

        when(fileChange.getType()).thenReturn(FileChange.FileChangeType.DELETION);
        changeRequestMergeDocumentResult = new ChangeRequestMergeDocumentResult(false, fileChange, "1.3", new Date(45));
        assertFalse(changeRequestMergeDocumentResult.hasConflicts());

        changeRequestMergeDocumentResult = new ChangeRequestMergeDocumentResult(true, fileChange, "1.3", new Date(45));
        assertTrue(changeRequestMergeDocumentResult.hasConflicts());
    }

    @Test
    void getIdentifier()
    {
        MergeDocumentResult mergeDocumentResult = mock(MergeDocumentResult.class);
        FileChange fileChange = mock(FileChange.class);
        when(fileChange.getVersion()).thenReturn("1.4-filechange");
        ChangeRequestMergeDocumentResult changeRequestMergeDocumentResult =
            new ChangeRequestMergeDocumentResult(mergeDocumentResult, fileChange, "1.3", new Date(45));

        assertEquals("1dot4-filechange_1dot3_45", changeRequestMergeDocumentResult.getIdentifier());
    }

    @Test
    void constructor()
    {
        FileChange fileChange = mock(FileChange.class);
        when(fileChange.getType()).thenReturn(FileChange.FileChangeType.DELETION);
        ChangeRequestMergeDocumentResult changeRequestMergeDocumentResult =
            new ChangeRequestMergeDocumentResult(false, fileChange, "1.3", new Date(45));
        assertEquals(fileChange, changeRequestMergeDocumentResult.getFileChange());

        when(fileChange.getType()).thenReturn(FileChange.FileChangeType.CREATION);
        changeRequestMergeDocumentResult =
            new ChangeRequestMergeDocumentResult(false, fileChange, "1.3", new Date(45));
        assertEquals(fileChange, changeRequestMergeDocumentResult.getFileChange());

        when(fileChange.getType()).thenReturn(FileChange.FileChangeType.EDITION);
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () ->
        {
            new ChangeRequestMergeDocumentResult(false, fileChange, "1.3", new Date(45));
        });
        assertEquals("This constructor should only be used for deletion or creation file changes.",
            illegalArgumentException.getMessage());
    }
}
