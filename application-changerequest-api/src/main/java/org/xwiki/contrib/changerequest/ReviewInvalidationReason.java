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

/**
 * Explain why a review has been invalidated.
 *
 * @version $Id$
 * @since 1.19
 */
public enum ReviewInvalidationReason
{
    /**
     * To be used when a review is manually invalidated through the UI.
     */
    MANUAL,

    /**
     * To be used when a review is invalidated because a new one is added.
     */
    NEW_REVIEW,

    /**
     * To be used when a review is invalidated because new change occurs.
     */
    NEW_CHANGE,

    /**
     * To be used when a review is invalidated because the approvers are updated.
     */
    UPDATED_APPROVERS,

    /**
     * To be used when a review is invalidated because a CR is split.
     */
    SPLITTED_CR
}
