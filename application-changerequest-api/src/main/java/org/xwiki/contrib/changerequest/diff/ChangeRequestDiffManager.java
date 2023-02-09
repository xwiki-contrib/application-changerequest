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
package org.xwiki.contrib.changerequest.diff;

import org.xwiki.component.annotation.Role;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.stability.Unstable;

/**
 * Component in charge of computation of diff.
 *
 * @version $Id$
 * @since 1.3
 */
@Unstable
@Role
public interface ChangeRequestDiffManager
{
    /**
     * Compute the html diff for the given file change.
     *
     * @param fileChange the file change for which to compute an html diff.
     * @return a diff ready to be displayed
     * @throws ChangeRequestException in case of problem for rendering the documents or computing the diff.
     */
    String getHtmlDiff(FileChange fileChange) throws ChangeRequestException;
}
