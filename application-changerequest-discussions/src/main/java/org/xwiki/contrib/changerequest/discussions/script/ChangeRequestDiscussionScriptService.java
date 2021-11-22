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
package org.xwiki.contrib.changerequest.discussions.script;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.discussions.ChangeRequestDiscussion;
import org.xwiki.contrib.changerequest.discussions.ChangeRequestDiscussionException;
import org.xwiki.contrib.changerequest.discussions.ChangeRequestDiscussionService;
import org.xwiki.contrib.changerequest.discussions.references.AbstractChangeRequestDiscussionContextReference;
import org.xwiki.contrib.changerequest.discussions.references.ChangeRequestCommentReference;
import org.xwiki.contrib.changerequest.discussions.references.ChangeRequestFileDiffReference;
import org.xwiki.contrib.changerequest.discussions.references.ChangeRequestLineDiffReference;
import org.xwiki.contrib.changerequest.discussions.references.ChangeRequestReference;
import org.xwiki.contrib.changerequest.discussions.references.FileDiffLocation;
import org.xwiki.contrib.changerequest.discussions.references.LineDiffLocation;
import org.xwiki.contrib.discussions.domain.Discussion;
import org.xwiki.contrib.discussions.domain.references.DiscussionReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;

/**
 * Script service for using discussions in change request.
 *
 * @version $Id$
 * @since 0.6
 */
@Unstable
@Component
@Named("changerequest.discussion")
@Singleton
public class ChangeRequestDiscussionScriptService implements ScriptService
{
    @Inject
    private ChangeRequestDiscussionService changeRequestDiscussionService;

    /**
     * Create a reference with the given information, to be used in {@link #createDiffDiscussion(
     * ChangeRequestFileDiffReference, LineDiffLocation.DiffDocumentPart, String, long, LineDiffLocation.LineChange)}.
     *
     * @param changeRequestId the identifier of the change request.
     * @param targetReference the reference of the document displayed in the diff
     * @param fileChangeId the identifier of the file change used in the diff
     * @param diffId the unique identifier of the diff
     * @return a reference that can be used to create discussion.
     * @since 0.7
     */
    public ChangeRequestFileDiffReference createFileDiffReference(String changeRequestId,
        String targetReference, String fileChangeId, String diffId)
    {
        FileDiffLocation fileDiffLocation = new FileDiffLocation(fileChangeId, diffId, targetReference);
        return new ChangeRequestFileDiffReference(changeRequestId, fileDiffLocation);
    }

    /**
     * Create or get a discussion for a diff line. All the arguments allow to uniquely identify where to attach the
     * discussion.
     *
     * @see ChangeRequestLineDiffReference
     * @param fileDiffReference the reference of the file diff where the discussion takes place
     * @param documentPart the part of the document diff where the discussion will be attached
     * @param documentPartLocation a more specific information to be used along the document part to know where to
     *                             attach the discussion. See {@link LineDiffLocation.DiffDocumentPart} for details.
     * @param lineNumber the line number of the document diff part where the discussion will be attached
     * @param lineChange the type of change of the line number to identify where to attach the discussion
     * @return the reference of the attached discussion
     * @throws ChangeRequestDiscussionException in case of problem when creating or getting the discussion
     */
    public DiscussionReference createDiffDiscussion(ChangeRequestFileDiffReference fileDiffReference,
        LineDiffLocation.DiffDocumentPart documentPart, String documentPartLocation, long lineNumber,
        LineDiffLocation.LineChange lineChange)
        throws ChangeRequestDiscussionException
    {
        LineDiffLocation lineDiffLocation = new LineDiffLocation(
            fileDiffReference.getFileDiffLocation(),
            documentPart,
            documentPartLocation,
            lineNumber,
            lineChange
        );
        ChangeRequestLineDiffReference reference = new ChangeRequestLineDiffReference(
            fileDiffReference.getChangeRequestId(),
            lineDiffLocation
        );
        return this.changeRequestDiscussionService.getOrCreateDiscussionFor(reference).getReference();
    }

    /**
     * Create or get a discussion for commenting a change request.
     *
     * @see ChangeRequestCommentReference
     * @param changeRequestId the id of the change request for which to create the discussion
     * @return the reference of the attached discussion
     * @throws ChangeRequestDiscussionException in case of problem when creating or getting the discussion
     */
    public DiscussionReference createChangeRequestCommentDiscussion(String changeRequestId)
        throws ChangeRequestDiscussionException
    {
        ChangeRequestCommentReference reference = new ChangeRequestCommentReference(changeRequestId);
        return this.changeRequestDiscussionService.getOrCreateDiscussionFor(reference).getReference();
    }

    private List<ChangeRequestDiscussion> getChangeRequestDiscussions(List<Discussion> discussions)
        throws ChangeRequestDiscussionException
    {
        List<ChangeRequestDiscussion> result = new ArrayList<>();
        for (Discussion discussion : discussions) {
            AbstractChangeRequestDiscussionContextReference reference =
                this.changeRequestDiscussionService.getReferenceFrom(discussion);
            result.add(new ChangeRequestDiscussion(reference, discussion));
        }
        return result;
    }

    /**
     * Retrieve all discussions attached to the given change request.
     *
     * @param changeRequest the change request for which to retrieve the discussions.
     * @return a list of discussions with their references.
     * @throws ChangeRequestDiscussionException in case of problem when querying the discussions.
     */
    public List<ChangeRequestDiscussion> getDiscussionsFromChangeRequest(ChangeRequest changeRequest)
        throws ChangeRequestDiscussionException
    {
        List<Discussion> discussions = this.changeRequestDiscussionService
            .getDiscussionsFrom(new ChangeRequestReference(changeRequest.getId()));
        return getChangeRequestDiscussions(discussions);
    }

    /**
     * Retrieve the change request discussion reference from the given discussion.
     *
     * @param discussion the discussion from which to retrieve the refererence.
     * @return an {@link AbstractChangeRequestDiscussionContextReference} corresponding to the discussion.
     * @throws ChangeRequestDiscussionException in case of problem to compute the exception
     * @since 0.7
     */
    public AbstractChangeRequestDiscussionContextReference getReference(Discussion discussion)
        throws ChangeRequestDiscussionException
    {
        return this.changeRequestDiscussionService.getReferenceFrom(discussion);
    }
}
