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

import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.FileChange;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Utility component for handling {@link FileChange} version scheme.
 *
 * @version $Id$
 * @since 0.4
 */
@Component(roles = FileChangeVersionManager.class)
@Singleton
public class FileChangeVersionManager
{
    /**
     * Check if the given version is a version of a document stored in a file change.
     * In practice this means that the version is prefixed with {@link FileChange#FILECHANGE_VERSION_PREFIX}.
     *
     * @param version the version string to check.
     * @return {@code true} if the given version concerns a file change document.
     */
    public boolean isFileChangeVersion(String version)
    {
        return StringUtils.startsWith(version, FileChange.FILECHANGE_VERSION_PREFIX);
    }

    /**
     * Compute next version and serialize it as string to be stored in a file change.
     *
     * @param version the current version to be bumped.
     * @param minor {@code true} if the next version should be minor, {@code false} if it should be major.
     * @return a string representing the next version for a file change.
     */
    public String getNextFileChangeVersion(String version, boolean minor)
    {
        Version previousVersion;
        boolean isFileChangeVersion = isFileChangeVersion(version);
        if (isFileChangeVersion) {
            previousVersion = new Version(version.substring(FileChange.FILECHANGE_VERSION_PREFIX.length()));
        } else {
            previousVersion = new Version(version);
        }
        Version nextVersion = XWikiDocument.getNextVersion(previousVersion, minor);
        return getFileChangeVersion(nextVersion.toString());
    }

    /**
     * Retrieve the actual {@link Version} from a string version, even if it's stored for a file change.
     *
     * @param version a string version stored in a file change.
     * @return a {@link Version} that can be manipulated for documents.
     */
    public Version getDocumentVersion(String version)
    {
        Version documentVersion;
        boolean isFileChangeVersion = isFileChangeVersion(version);
        if (isFileChangeVersion) {
            documentVersion = new Version(version.substring(FileChange.FILECHANGE_VERSION_PREFIX.length()));
        } else {
            documentVersion = new Version(version);
        }
        return documentVersion;
    }

    /**
     * Prefix the given version with a {@link FileChange#FILECHANGE_VERSION_PREFIX} if it's not done yet.
     *
     * @param version the version to be prefixed.
     * @return a version prefixed.
     */
    public String getFileChangeVersion(String version)
    {
        return (isFileChangeVersion(version)) ? version : FileChange.FILECHANGE_VERSION_PREFIX + version;
    }
}
