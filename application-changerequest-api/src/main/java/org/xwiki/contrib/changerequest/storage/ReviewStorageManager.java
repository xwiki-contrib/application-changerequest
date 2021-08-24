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
package org.xwiki.contrib.changerequest.storage;

import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestReview;
import org.xwiki.stability.Unstable;

/**
 * Component role for storing the {@link ChangeRequestReview}.
 *
 * @version $Id$
 * @since 0.4
 */
@Role
@Unstable
public interface ReviewStorageManager
{
    /**
     * Save the given review.
     *
     * @param review the review to be saved.
     * @throws ChangeRequestException in case of problem during the save.
     */
    void save(ChangeRequestReview review) throws ChangeRequestException;

    /**
     * Load all reviews related to the given change request. Note that the method should also set the reviews in
     * the change request object so that {@link ChangeRequest#getReviews()} then returns the loaded reviews.
     *
     * @param changeRequest the change request for which to load the reviews.
     * @return a list of loaded reviews.
     * @throws ChangeRequestException in case of problem during the load.
     */
    List<ChangeRequestReview> load(ChangeRequest changeRequest) throws ChangeRequestException;


}
