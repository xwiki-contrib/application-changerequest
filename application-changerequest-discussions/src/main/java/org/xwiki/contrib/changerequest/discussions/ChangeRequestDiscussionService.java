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
package org.xwiki.contrib.changerequest.discussions;

import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.discussions.references.AbstractChangeRequestDiscussionContextReference;
import org.xwiki.contrib.discussions.domain.Discussion;
import org.xwiki.contrib.discussions.domain.DiscussionContext;
import org.xwiki.stability.Unstable;

/**
 * Component for dealing with discussions in change request.
 *
 * @version $Id$
 * @since 0.6
 */
@Unstable
@Role
public interface ChangeRequestDiscussionService
{
    /**
     * Hint used for change request in discussions: this hint should be used in all discussion references.
     */
    String APPLICATION_HINT = "changerequest";

    /**
     * Allow to get or create a discussion based on the given reference.
     * This method should get or create the underlying contexts corresponding to the reference and associate them to the
     * created discussion.
     *
     * @param reference the reference for which to create the discussion
     * @param <T> the concrete type of the reference
     * @return the discussion created or retrieved
     * @throws ChangeRequestDiscussionException in case of problem to create or retrieve one of the element
     */
    <T extends AbstractChangeRequestDiscussionContextReference> Discussion getOrCreateDiscussionFor(T reference)
        throws ChangeRequestDiscussionException;

    /**
     * Retrieve discussions related to the given reference, without creating any.
     *
     * @param reference the reference for which to retrieve the discussions
     * @param <T> the concrete type of the reference
     * @return a list of discussions created for this reference
     * @throws ChangeRequestDiscussionException in case of problem for requesting the discussions
     */
    <T extends AbstractChangeRequestDiscussionContextReference> List<Discussion> getDiscussionsFrom(T reference)
        throws ChangeRequestDiscussionException;

    /**
     * Compute a leaf reference based on the given discussion.
     * This method should be used to determine what's the fine-grained reference related to the given discussion's
     * contexts, in order to attach the discussion to the proper UI element.
     *
     * @param discussion the discussion for which to find the reference
     * @return the most specific reference that can be inferred from the contexts of the discussion
     * @throws ChangeRequestDiscussionException in case of problem when requesting the contexts or when computing
     *                                          the reference
     */
    AbstractChangeRequestDiscussionContextReference getReferenceFrom(Discussion discussion)
        throws ChangeRequestDiscussionException;

    /**
     * Allow to move all discussions related to the original change request, in the given splitted change requests.
     * Global discussions should be copied in all splitted change requests, while specific file discussions should
     * target specific change request.
     *
     * @param originalChangeRequest the original change request containing all discussions.
     * @param splittedChangeRequest the splitted change requests where to copy the discussions.
     * @throws ChangeRequestDiscussionException in case of problem during the move.
     * @since 0.7
     */
    default void moveDiscussions(ChangeRequest originalChangeRequest, List<ChangeRequest> splittedChangeRequest) throws
        ChangeRequestDiscussionException
    {
    }

    /**
     * Allow to get or create a discussion context based on the given reference.
     *
     * @param reference the reference for which to create the discussion context
     * @param <T> the concrete type of the reference
     * @return the discussion context created or retrieved
     * @throws ChangeRequestDiscussionException in case of problem to create or retrieve one of the element
     */
    default <T extends AbstractChangeRequestDiscussionContextReference> DiscussionContext
        getOrCreateDiscussionContextFor(T reference) throws ChangeRequestDiscussionException
    {
        return null;
    }
}
