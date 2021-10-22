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
package org.xwiki.contrib.changerequest.internal;

import org.junit.jupiter.api.Test;
import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link FileChangeVersionManager}.
 *
 * @version $Id$
 * @since 0.6
 */
@ComponentTest
class FileChangeVersionManagerTest
{
    @InjectMockComponents
    private FileChangeVersionManager versionManager;

    @Test
    void isFileChangeVersion()
    {
        assertFalse(versionManager.isFileChangeVersion(null));
        assertFalse(versionManager.isFileChangeVersion(""));
        assertFalse(versionManager.isFileChangeVersion("null"));
        assertFalse(versionManager.isFileChangeVersion("1.4"));
        assertTrue(versionManager.isFileChangeVersion("filechange-1.4"));
    }

    @Test
    void getNextFileChangeVersion()
    {
        assertEquals("filechange-1.5", versionManager.getNextFileChangeVersion("1.4", true));
        assertEquals("filechange-2.1", versionManager.getNextFileChangeVersion("1.4", false));

        assertEquals("filechange-1.5", versionManager.getNextFileChangeVersion("filechange-1.4", true));
        assertEquals("filechange-2.1", versionManager.getNextFileChangeVersion("filechange-1.4", false));
    }

    @Test
    void getDocumentVersion()
    {
        assertEquals(new Version("1.4"), versionManager.getDocumentVersion("1.4"));
        assertEquals(new Version("1.4"), versionManager.getDocumentVersion("filechange-1.4"));
    }

    @Test
    void getFileChangeVersion()
    {
        assertEquals("filechange-1.4", versionManager.getFileChangeVersion("1.4"));
        assertEquals("filechange-1.4", versionManager.getFileChangeVersion("filechange-1.4"));
    }
}
