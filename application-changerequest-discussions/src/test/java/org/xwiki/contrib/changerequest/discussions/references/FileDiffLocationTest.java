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

public class FileDiffLocationTest
{
    @Test
    void getSerializedReference()
    {
        FileDiffLocation fileDiffLocation =
            new FileDiffLocation("filechange-3.2-243435", "34343", "xwiki:Main.WebHome");
        String serializedReference = fileDiffLocation.getSerializedReference();
        assertEquals("xwiki:Main.WebHome_filechange-3.2-243435_34343", serializedReference);
    }

    @Test
    void isMatching()
    {
        assertTrue(FileDiffLocation.isMatching("xwiki:Main.WebHome_filechange-3.2-243435_34343"));
        assertTrue(FileDiffLocation.isMatching("Main.WebHome_filechange-3.2-243435_34343"));
        assertTrue(FileDiffLocation.isMatching("Main12365_455.WebHome_filechange-653.6-243435_34343"));
        assertFalse(FileDiffLocation.isMatching("filechange-653.6-243435_34343"));
        assertFalse(FileDiffLocation.isMatching("Main.WebHome_filechange-3.2"));
    }

    @Test
    void parse()
    {
        FileDiffLocation fileDiffLocation = FileDiffLocation.parse("xwiki:Main.WebHome_filechange-3.2-243435_34343");
        FileDiffLocation expected =
            new FileDiffLocation("filechange-3.2-243435", "34343", "xwiki:Main.WebHome");
        assertEquals(expected, fileDiffLocation);

        fileDiffLocation = FileDiffLocation.parse("Main12365_455.WebHome_filechange-653.6-243435_34343");
        expected =
            new FileDiffLocation("filechange-653.6-243435", "34343", "Main12365_455.WebHome");
        assertEquals(expected, fileDiffLocation);
    }
}
