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

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Role;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.stability.Unstable;

/**
 * Allow to provide different implementation for rendering content of document for computing the diff.
 * The purpose of the component is to have various implementations to allow a trade-off between security and
 * utilisability for the rendered diff.
 *
 * @version $Id$
 * @since 1.5
 * @since 1.4.4
 */
@Unstable
@Role
public interface ChangeRequestDiffRenderContent
{
    /**
     * Perform an HTML rendering of the content of the given document: the obtained rendering is then used to compute
     * the diff in {@link ChangeRequestDiffManager}.
     *
     * @param document the document to be rendered
     * @param fileChange the {@link FileChange} related to that document to obtain some metadata such as the author
     * @return the rendered HTML of the content of the document
     * @throws ChangeRequestException in case of problem when performing the rendering
     */
    String getRenderedContent(DocumentModelBridge document, FileChange fileChange) throws ChangeRequestException;
}
