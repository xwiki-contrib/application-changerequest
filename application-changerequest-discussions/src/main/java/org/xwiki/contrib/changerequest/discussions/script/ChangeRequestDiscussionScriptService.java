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

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.discussions.ChangeRequestDiscussion;
import org.xwiki.contrib.changerequest.discussions.ChangeRequestDiscussionException;
import org.xwiki.contrib.changerequest.discussions.ChangeRequestDiscussionService;
import org.xwiki.contrib.changerequest.discussions.internal.ChangeRequestDiscussionDiffUtils;
import org.xwiki.contrib.changerequest.discussions.references.AbstractChangeRequestDiscussionContextReference;
import org.xwiki.contrib.changerequest.discussions.references.ChangeRequestCommentReference;
import org.xwiki.contrib.changerequest.discussions.references.ChangeRequestFileDiffReference;
import org.xwiki.contrib.changerequest.discussions.references.ChangeRequestLineDiffReference;
import org.xwiki.contrib.changerequest.discussions.references.ChangeRequestReference;
import org.xwiki.contrib.changerequest.discussions.references.ChangeRequestReviewReference;
import org.xwiki.contrib.changerequest.discussions.references.ChangeRequestReviewsReference;
import org.xwiki.contrib.changerequest.discussions.references.difflocation.FileDiffLocation;
import org.xwiki.contrib.changerequest.discussions.references.difflocation.LineDiffLocation;
import org.xwiki.contrib.discussions.domain.Discussion;
import org.xwiki.contrib.discussions.domain.references.DiscussionContextReference;
import org.xwiki.contrib.discussions.domain.references.DiscussionReference;
import org.xwiki.diff.display.UnifiedDiffBlock;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.properties.ConverterManager;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;

import com.fasterxml.jackson.core.JsonProcessingException;

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
    private static final String UNDERSCORE = "_";

    @Inject
    private ChangeRequestDiscussionService changeRequestDiscussionService;

    @Inject
    private ConverterManager converterManager;

    @Inject
    private ChangeRequestDiscussionDiffUtils changeRequestDiscussionDiffUtils;

    /**
     * Create a reference with the given information, to be used in for creating diff discussion.
     *
     * @param changeRequestId the identifier of the change request.
     * @param targetReference the reference of the document displayed in the diff
     * @param diffId the unique identifier of the diff
     * @return a reference that can be used to create discussion.
     * @since 0.7
     */
    public ChangeRequestFileDiffReference getOrCreateFileDiffReference(String changeRequestId,
        String targetReference, String diffId)
    {
        FileDiffLocation fileDiffLocation = new FileDiffLocation(diffId, targetReference);
        return new ChangeRequestFileDiffReference(changeRequestId, fileDiffLocation);
    }

    /**
     * Create or get a discussion for a diff line. All the arguments allow to uniquely identify where to attach the
     * discussion.
     *
     * @see ChangeRequestLineDiffReference
     * @param fileDiffReference the reference of the file diff where the discussion takes place
     * @param entityReference the reference of the specific entity where the discussion takes place, e.g. an xobject or
     *                        an xclass
     * @param diffBlockId the specific name of the property discussed
     * @param lineNumber the line number of the document diff part where the discussion will be attached
     * @param lineChange the type of change of the line number to identify where to attach the discussion
     * @return the reference of the attached discussion
     * @throws ChangeRequestDiscussionException in case of problem when creating or getting the discussion
     */
    public DiscussionReference getOrCreateDiffDiscussion(ChangeRequestFileDiffReference fileDiffReference,
        EntityReference entityReference, String diffBlockId, long lineNumber,
        LineDiffLocation.LineChange lineChange)
        throws ChangeRequestDiscussionException
    {
        LineDiffLocation.DiffDocumentPart documentPart;
        // We rely on the ConverterManager and not on the serializer here, to keep the syntax containing the
        // entity reference type.
        String serializedEntityReference = this.converterManager.convert(String.class, entityReference);
        if (serializedEntityReference.equals(fileDiffReference.getReference())) {
            serializedEntityReference = UNDERSCORE;
            documentPart = LineDiffLocation.DiffDocumentPart.METADATA;
        } else {
            switch (entityReference.getType()) {
                case OBJECT_PROPERTY:
                    documentPart = LineDiffLocation.DiffDocumentPart.XOBJECT;
                    break;

                case CLASS_PROPERTY:
                    documentPart = LineDiffLocation.DiffDocumentPart.XCLASS;
                    break;

                default:
                    documentPart = LineDiffLocation.DiffDocumentPart.METADATA;
                    break;
            }
        }
        String diffBlockIdNormalized = (StringUtils.isEmpty(diffBlockId)) ? UNDERSCORE : diffBlockId;

        LineDiffLocation lineDiffLocation = new LineDiffLocation(
            fileDiffReference.getFileDiffLocation(),
            documentPart,
            serializedEntityReference,
            diffBlockIdNormalized,
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
    public DiscussionReference getOrCreateChangeRequestCommentDiscussion(String changeRequestId)
        throws ChangeRequestDiscussionException
    {
        ChangeRequestCommentReference reference = new ChangeRequestCommentReference(changeRequestId);
        return this.changeRequestDiscussionService.getOrCreateDiscussionFor(reference).getReference();
    }

    /**
     * Create or get a discussion for a specific review of a change request.
     *
     * @see ChangeRequestReviewReference
     * @param changeRequestId the id of the change request for which to create the discussion
     * @param reviewId the id of the change request for which to create the discussion
     * @return the reference of the attached discussion
     * @throws ChangeRequestDiscussionException in case of problem when creating or getting the discussion
     * @since 0.8
     */
    public DiscussionReference getOrCreateChangeRequestReviewDiscussion(String changeRequestId, String reviewId)
        throws ChangeRequestDiscussionException
    {
        ChangeRequestReviewReference reference = new ChangeRequestReviewReference(reviewId, changeRequestId);
        return this.changeRequestDiscussionService.getOrCreateDiscussionFor(reference).getReference();
    }

    /**
     * Create a new discussion for all reviews of a change request.
     *
     * @see ChangeRequestReviewsReference
     * @param changeRequestId the id of the change request for which to create the discussion
     * @return the reference of the attached discussion
     * @throws ChangeRequestDiscussionException in case of problem when creating or getting the discussion
     * @since 0.8
     */
    public DiscussionReference createChangeRequestReviewsDiscussion(String changeRequestId)
        throws ChangeRequestDiscussionException
    {
        ChangeRequestReviewsReference reference = new ChangeRequestReviewsReference(changeRequestId);
        return this.changeRequestDiscussionService.createDiscussionFor(reference).getReference();
    }

    /**
     * Get or create a discussion context for a specific review.
     * Note that this method returns a {@link DiscussionContextReference} and not {@link DiscussionReference} because
     * discussions for reviews are created globally for all reviews with
     * {@link #createChangeRequestReviewsDiscussion(String)} and then attached to the proper context with this method.
     *
     * @param changeRequestId the id of the change request for which to create the discussion context
     * @param reviewId the id of the change request for which to create the discussion context
     * @return a reference to the {@link org.xwiki.contrib.discussions.domain.DiscussionContext} to attach an existing
     *         discussion.
     * @throws ChangeRequestDiscussionException in case of problem when creating or getting the discussion
     * @since 0.8
     */
    public DiscussionContextReference getOrCreateChangeRequestReviewDiscussionContext(String changeRequestId,
        String reviewId) throws ChangeRequestDiscussionException
    {
        ChangeRequestReviewReference reference = new ChangeRequestReviewReference(reviewId, changeRequestId);
        return this.changeRequestDiscussionService.getOrCreateDiscussionContextFor(reference).getReference();
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

    /**
     * Deserialize the given context diff and attach it to the discussion.
     * If the discussion does not reference a line diff, the method will return {@code false}.
     *
     * @param discussionReference the reference of the discussion for which to attach a context diff
     * @param contextDiff a serialized representation of a {@link UnifiedDiffBlock}
     * @return {@code true} if the block has been properly attached to the discussion context
     * @throws ChangeRequestDiscussionException in case of problem for processing the block
     * @throws JsonProcessingException in case of problem for parsing the given serialization
     * @since 1.5
     */
    public boolean attachDiffBlockMetadata(DiscussionReference discussionReference, String contextDiff)
        throws ChangeRequestDiscussionException, JsonProcessingException
    {
        if (!StringUtils.isEmpty(contextDiff)) {
            UnifiedDiffBlock<String, Character> diffBlock =
                this.changeRequestDiscussionDiffUtils.deserialize(contextDiff);
            return this.changeRequestDiscussionService.attachDiffBlockMetadata(discussionReference, diffBlock);
        } else {
            return false;
        }
    }

    /**
     * Try to retrieve a diff block metadata for the given discussion.
     * This method should check if the discussion has a line diff context, and check in the metadata of this context
     * if there's an attached diff block, in which case it will be deserialized and returned.
     *
     * @param changeRequestDiscussion the discussion for which to find the context metadata
     * @return the attached {@link UnifiedDiffBlock} or {@code null}
     * @throws ChangeRequestException in case of problem when deserializing the attached block
     * @since 1.5
     */
    public UnifiedDiffBlock<String, Character> getDiffBlockMetadata(ChangeRequestDiscussion changeRequestDiscussion)
        throws ChangeRequestException
    {
        if (changeRequestDiscussion.getReference() instanceof ChangeRequestLineDiffReference) {
            return this.changeRequestDiscussionService.getDiffBlockMetadata(changeRequestDiscussion.getDiscussion())
                .orElse(null);
        } else {
            return null;
        }
    }
}
