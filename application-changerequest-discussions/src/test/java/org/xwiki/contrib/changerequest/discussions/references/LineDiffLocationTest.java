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
package org.xwiki.contrib.changerequest.discussions.references;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LineDiffLocationTest
{
    @Test
    void getSerializedReference()
    {
        FileDiffLocation fileDiffLocation = mock(FileDiffLocation.class);
        when(fileDiffLocation.getSerializedReference()).thenReturn("xx_x_yyy_zzz");
        LineDiffLocation lineDiffLocation = new LineDiffLocation(
            fileDiffLocation,
            LineDiffLocation.DiffDocumentPart.CONTENT,
            "content",
            134L,
            LineDiffLocation.LineChange.UNCHANGED);
        assertEquals("xx_x_yyy_zzz_CONTENT_content_UNCHANGED_134", lineDiffLocation.getSerializedReference());
    }

    @Test
    void isMatching()
    {
        assertTrue(LineDiffLocation.isMatching("xx_x_yyy_zzz_CONTENT_content_UNCHANGED_134"));
        assertTrue(LineDiffLocation.isMatching("xx_x_yyy_zzz_XOBJECT_XWiki.XWikiUser#name_ADDED_1"));
        assertFalse(LineDiffLocation.isMatching("xx_x_yyy_zzz_CONTENT_UNCHANGED_134"));
        assertFalse(LineDiffLocation.isMatching("CONTENT_ef_UNCHANGED_134"));
    }

    @Test
    void parse()
    {
        LineDiffLocation lineDiffLocation = LineDiffLocation.parse("xx_x_yyy_zzz_CONTENT_content_UNCHANGED_134");

        FileDiffLocation expectedFileDiff = new FileDiffLocation("yyy", "zzz", "xx_x");
        LineDiffLocation expected = new LineDiffLocation(
            expectedFileDiff,
            LineDiffLocation.DiffDocumentPart.CONTENT,
            "content",
            134,
            LineDiffLocation.LineChange.UNCHANGED);
        assertEquals(expected, lineDiffLocation);

        lineDiffLocation = LineDiffLocation.parse("xx_x_yyy_zzz_XOBJECT_XWiki.XWikiUser#name_ADDED_1");
        expected = new LineDiffLocation(
            expectedFileDiff,
            LineDiffLocation.DiffDocumentPart.XOBJECT,
            "XWiki.XWikiUser#name",
            1,
            LineDiffLocation.LineChange.ADDED);
        assertEquals(expected, lineDiffLocation);
    }
}
