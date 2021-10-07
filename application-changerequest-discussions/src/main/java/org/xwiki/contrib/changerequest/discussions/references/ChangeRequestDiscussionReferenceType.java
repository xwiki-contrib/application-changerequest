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

import org.xwiki.stability.Unstable;

/**
 * Define the different types of change request references.
 *
 * @version $Id$
 * @since 0.6
 */
@Unstable
public enum ChangeRequestDiscussionReferenceType
{
    /**
     * Represents the change request itself.
     */
    CHANGE_REQUEST,

    /**
     * Represents any comment of the change request, not attached to a particular review or diff.
     */
    CHANGE_REQUEST_COMMENT,

    /**
     * Represents a specific review.
     */
    REVIEW,

    /**
     * Represents any review.
     */
    REVIEWS,

    /**
     * Represents a specific file diff.
     */
    FILE_DIFF,

    /**
     * Represents a specific line of a file diff.
     */
    LINE_DIFF
}
