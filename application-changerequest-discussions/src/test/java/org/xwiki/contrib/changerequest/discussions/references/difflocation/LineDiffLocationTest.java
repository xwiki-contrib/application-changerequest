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
package org.xwiki.contrib.changerequest.discussions.references.difflocation;

import org.junit.jupiter.api.Test;
import org.xwiki.contrib.changerequest.discussions.references.difflocation.FileDiffLocation;
import org.xwiki.contrib.changerequest.discussions.references.difflocation.LineDiffLocation;

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
        FileDiffLocation fileDiffLocation = new FileDiffLocation("filechange-1.1_2.3_484848", "xwiki:Main.WebHome");
        LineDiffLocation lineDiffLocation = new LineDiffLocation(
            fileDiffLocation,
            LineDiffLocation.DiffDocumentPart.METADATA,
            "_",
            "content",
            134L,
            LineDiffLocation.LineChange.UNCHANGED);
        assertEquals("xwiki:Main.WebHome/filechange-1.1_2.3_484848/METADATA/_/content/UNCHANGED/134",
            lineDiffLocation.getSerializedReference());

        lineDiffLocation = new LineDiffLocation(
            fileDiffLocation,
            LineDiffLocation.DiffDocumentPart.XOBJECT,
            "xwiki:Main.WebHome^XWiki.StyleSheetExtension[0]",
            "code",
            38,
            LineDiffLocation.LineChange.ADDED);
        assertEquals("xwiki:Main.WebHome/filechange-1.1_2.3_484848/XOBJECT/"
                + "xwiki:Main.WebHome^XWiki.StyleSheetExtension[0]/code/ADDED/38",
            lineDiffLocation.getSerializedReference());

        fileDiffLocation = new FileDiffLocation("filechange-1.1_2.3_484848", "xwiki:Main.Web/Home");

        lineDiffLocation = new LineDiffLocation(
            fileDiffLocation,
            LineDiffLocation.DiffDocumentPart.XCLASS,
            "xwiki:Main.Web/Home^to\\to",
            "Hint",
            38,
            LineDiffLocation.LineChange.REMOVED);
        assertEquals("xwiki:Main.Web\\/Home/filechange-1.1_2.3_484848/XCLASS/"
                + "xwiki:Main.Web\\/Home^to\\\\to/Hint/REMOVED/38",
            lineDiffLocation.getSerializedReference());
    }

    @Test
    void parse()
    {
        LineDiffLocation lineDiffLocation =
            LineDiffLocation.parse("xwiki:Main.WebHome/filechange-1.1_2.3_484848/METADATA/_/content/UNCHANGED/134");

        FileDiffLocation fileDiffLocation = new FileDiffLocation("filechange-1.1_2.3_484848", "xwiki:Main.WebHome");
        LineDiffLocation expected = new LineDiffLocation(
            fileDiffLocation,
            LineDiffLocation.DiffDocumentPart.METADATA,
            "_",
            "content",
            134L,
            LineDiffLocation.LineChange.UNCHANGED);

        assertEquals(expected, lineDiffLocation);

        lineDiffLocation =
            LineDiffLocation.parse("xwiki:Main.WebHome/filechange-1.1_2.3_484848/XOBJECT/"
                + "xwiki:Main.WebHome^XWiki.StyleSheetExtension[0]/code/ADDED/38");
        expected = new LineDiffLocation(
            fileDiffLocation,
            LineDiffLocation.DiffDocumentPart.XOBJECT,
            "xwiki:Main.WebHome^XWiki.StyleSheetExtension[0]",
            "code",
            38,
            LineDiffLocation.LineChange.ADDED);
        assertEquals(expected, lineDiffLocation);

        fileDiffLocation = new FileDiffLocation("filechange-1.1_2.3_484848", "xwiki:Main.Web/Home");
        lineDiffLocation =
            LineDiffLocation.parse("xwiki:Main.Web\\/Home/filechange-1.1_2.3_484848/XCLASS/"
                + "xwiki:Main.Web\\/Home^to\\\\to/Hint/REMOVED/38");
        expected = new LineDiffLocation(
            fileDiffLocation,
            LineDiffLocation.DiffDocumentPart.XCLASS,
            "xwiki:Main.Web/Home^to\\to",
            "Hint",
            38,
            LineDiffLocation.LineChange.REMOVED);
        assertEquals(expected, lineDiffLocation);
    }
}
