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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileDiffLocationTest
{
    @Test
    void getSerializedReference()
    {
        FileDiffLocation fileDiffLocation =
            new FileDiffLocation("filechange-1.1_2.3_484848", "xwiki:Main.WebHome");
        String serializedReference = fileDiffLocation.getSerializedReference();
        assertEquals("xwiki:Main.WebHome/filechange-1.1_2.3_484848", serializedReference);

        fileDiffLocation =
            new FileDiffLocation("filechange-1.1_2.3_484848", "xwiki:Mai/n.Web\\Home");
        serializedReference = fileDiffLocation.getSerializedReference();
        assertEquals("xwiki:Mai\\/n.Web\\\\Home/filechange-1.1_2.3_484848", serializedReference);
    }

    @Test
    void parse()
    {
        FileDiffLocation fileDiffLocation = FileDiffLocation.parse("xwiki:Main.WebHome/filechange-1.1_2.3_484848");
        FileDiffLocation expected =
            new FileDiffLocation("filechange-1.1_2.3_484848", "xwiki:Main.WebHome");
        assertEquals(expected, fileDiffLocation);

        fileDiffLocation = FileDiffLocation.parse("xwiki:Mai\\/n.Web\\\\Home/filechange-1.1_2.3_484848");
        expected =
            new FileDiffLocation("filechange-1.1_2.3_484848", "xwiki:Mai/n.Web\\Home");
        assertEquals(expected, fileDiffLocation);
    }
}
