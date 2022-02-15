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

import org.xwiki.stability.Unstable;

/**
 * Represents the different statuses possible for a change request.
 *
 * @version $Id$
 * @since 0.1
 */
@Unstable
public enum ChangeRequestStatus
{
    /**
     * When the change request is still a draft and cannot be merged.
     */
    DRAFT(true),

    /**
     * When the change request has been merged.
     */
    MERGED(false),

    /**
     * When the change request can be reviewed.
     * @since 0.4
     */
    READY_FOR_REVIEW(true),

    /**
     * When the change request can be merged.
     * @since 0.6
     */
    READY_FOR_MERGING(true),

    /**
     * When the change request is closed without merging.
     * @since 0.6
     */
    CLOSED(false),

    /**
     * When the  change request is closed because it's stale.
     * @since 0.10
     */
    STALE(false);

    private final boolean isOpen;

    /**
     * Default constructor.
     *
     * @param isOpen {@code true} if the status means that the change request is still open, {@code false} if it means
     *               it is closed.
     */
    ChangeRequestStatus(boolean isOpen)
    {
        this.isOpen = isOpen;
    }

    /**
     * @return {@code true} if the status means that the change request is still open and can be edited,
     *         {@code false} if it means it is closed.
     */
    public boolean isOpen()
    {
        return this.isOpen;
    }
}
