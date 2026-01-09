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
package org.xwiki.contrib.changerequest.discussions.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.discussions.ChangeRequestDiscussionDiffBlock;
import org.xwiki.contrib.changerequest.discussions.ChangeRequestDiscussionException;
import org.xwiki.contrib.changerequest.discussions.ChangeRequestDiscussionService;
import org.xwiki.contrib.changerequest.discussions.references.AbstractChangeRequestDiscussionContextReference;
import org.xwiki.contrib.changerequest.discussions.references.ChangeRequestCommentReference;
import org.xwiki.contrib.changerequest.discussions.references.ChangeRequestFileDiffReference;
import org.xwiki.contrib.changerequest.discussions.references.ChangeRequestLineDiffReference;
import org.xwiki.contrib.changerequest.discussions.references.ChangeRequestReference;
import org.xwiki.contrib.changerequest.discussions.references.ChangeRequestReviewReference;
import org.xwiki.contrib.changerequest.discussions.references.ChangeRequestReviewsReference;
import org.xwiki.contrib.changerequest.discussions.references.difflocation.FileDiffLocation;
import org.xwiki.contrib.discussions.DiscussionContextService;
import org.xwiki.contrib.discussions.DiscussionException;
import org.xwiki.contrib.discussions.DiscussionService;
import org.xwiki.contrib.discussions.domain.Discussion;
import org.xwiki.contrib.discussions.domain.DiscussionContext;
import org.xwiki.contrib.discussions.domain.references.DiscussionReference;
import org.xwiki.diff.display.UnifiedDiffBlock;
import org.xwiki.model.reference.EntityReferenceSerializer;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Default implementation of {@link ChangeRequestDiscussionService}.
 *
 * @version $Id$
 * @since 0.6
 */
@Component
@Singleton
public class DefaultChangeRequestDiscussionService implements ChangeRequestDiscussionService
{
    @Inject
    private DiscussionContextService discussionContextService;

    @Inject
    private DiscussionService discussionService;

    @Inject
    private ChangeRequestDiscussionReferenceUtils discussionReferenceUtils;

    @Inject
    private ChangeRequestDiscussionFactory changeRequestDiscussionFactory;

    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> stringEntityReferenceSerializer;

    @Inject
    private ChangeRequestDiscussionDiffUtils changeRequestDiscussionDiffUtils;

    @Inject
    private Logger logger;

    private <T extends AbstractChangeRequestDiscussionContextReference> List<DiscussionContext> getContextListFor(
        T reference) throws ChangeRequestDiscussionException
    {
        List<DiscussionContext> contextList = new ArrayList<>();

        contextList.add(this.changeRequestDiscussionFactory.getOrCreateContextFor(
            new ChangeRequestReference(reference.getChangeRequestId())));
        switch (reference.getType()) {
            case CHANGE_REQUEST_COMMENT:
            case REVIEWS:
            case FILE_DIFF:
                contextList.add(this.changeRequestDiscussionFactory.getOrCreateContextFor(reference));
                break;

            case REVIEW:
                contextList.add(this.changeRequestDiscussionFactory.getOrCreateContextFor(reference));
                contextList.add(this.changeRequestDiscussionFactory.getOrCreateContextFor(
                    new ChangeRequestReviewsReference(reference.getChangeRequestId())));
                break;

            case LINE_DIFF:
                contextList.add(this.changeRequestDiscussionFactory.getOrCreateContextFor(reference));
                ChangeRequestLineDiffReference lineDiffReference = (ChangeRequestLineDiffReference) reference;
                contextList.add(this.changeRequestDiscussionFactory.getOrCreateContextFor(
                    new ChangeRequestFileDiffReference(lineDiffReference.getChangeRequestId(),
                        lineDiffReference.getLineDiffLocation().getFileDiffLocation())));
                break;

            case CHANGE_REQUEST:
            default:
                break;
        }
        return contextList;
    }

    @Override
    public <T extends AbstractChangeRequestDiscussionContextReference> Discussion getOrCreateDiscussionFor(T reference)
        throws ChangeRequestDiscussionException
    {
        List<DiscussionContext> contextList = this.getContextListFor(reference);

        try {
            return this.discussionService.getOrCreate(
                ChangeRequestDiscussionService.APPLICATION_HINT,
                this.discussionReferenceUtils.getTitleTranslation(
                    ChangeRequestDiscussionReferenceUtils.DISCUSSION_TRANSLATION_PREFIX, reference),
                this.discussionReferenceUtils.getDescriptionTranslation(
                    ChangeRequestDiscussionReferenceUtils.DISCUSSION_TRANSLATION_PREFIX, reference),
                contextList.stream().map(DiscussionContext::getReference).collect(Collectors.toList()),
                this.changeRequestDiscussionFactory.createDiscussionStoreConfigurationParametersFor(reference)
            );
        } catch (DiscussionException e) {
            throw new ChangeRequestDiscussionException(
                String.format("Error while getting or creating discussion for reference [%s]", reference), e);
        }
    }

    @Override
    public <T extends AbstractChangeRequestDiscussionContextReference> Discussion createDiscussionFor(T reference)
        throws ChangeRequestDiscussionException
    {
        List<DiscussionContext> contextList = this.getContextListFor(reference);
        if (reference.isUniqueDiscussion()) {
            List<Discussion> discussions = this.discussionService.findByDiscussionContexts(
                contextList.stream()
                    .map(DiscussionContext::getReference)
                    .collect(Collectors.toList()));
            if (!discussions.isEmpty()) {
                throw new ChangeRequestDiscussionException(
                    String.format("Discussion already exists for reference [%s].", reference));
            }
        }
        try {
            Discussion discussion = this.discussionService.create(
                ChangeRequestDiscussionService.APPLICATION_HINT,
                this.discussionReferenceUtils.getTitleTranslation(
                    ChangeRequestDiscussionReferenceUtils.DISCUSSION_TRANSLATION_PREFIX, reference),
                this.discussionReferenceUtils.getDescriptionTranslation(
                    ChangeRequestDiscussionReferenceUtils.DISCUSSION_TRANSLATION_PREFIX, reference),
                null,
                this.changeRequestDiscussionFactory.createDiscussionStoreConfigurationParametersFor(reference));
            for (DiscussionContext discussionContext : contextList) {
                this.discussionContextService.link(discussionContext, discussion);
            }
            return discussion;
        } catch (DiscussionException e) {
            throw new ChangeRequestDiscussionException(
                String.format("Error while creating discussion for reference [%s]", reference), e);
        }
    }

    @Override
    public <T extends AbstractChangeRequestDiscussionContextReference> List<Discussion> getDiscussionsFrom(T reference)
        throws ChangeRequestDiscussionException
    {
        if (this.changeRequestDiscussionFactory.isContextExistingFor(reference)) {
            DiscussionContext discussionContext = this.changeRequestDiscussionFactory.getOrCreateContextFor(reference);
            return this.discussionService
                .findByDiscussionContexts(Collections.singletonList(discussionContext.getReference()));
        } else {
            return List.of();
        }
    }

    @Override
    public AbstractChangeRequestDiscussionContextReference getReferenceFrom(Discussion discussion)
        throws ChangeRequestDiscussionException
    {
        List<DiscussionContext> discussionContexts =
            this.discussionContextService.findByDiscussionReference(discussion.getReference());
        AbstractChangeRequestDiscussionContextReference reference = null;

        for (DiscussionContext discussionContext : discussionContexts) {
            reference = this.discussionReferenceUtils.computeReferenceFromContext(discussionContext, reference);
        }

        if (reference != null) {
            return reference;
        } else {
            throw new ChangeRequestDiscussionException(
                String.format("Error while computing reference for contexts [%s] from discussion [%s]",
                    discussionContexts, discussion));
        }
    }

    @Override
    public void moveDiscussions(ChangeRequest originalChangeRequest, List<ChangeRequest> splittedChangeRequests)
        throws ChangeRequestDiscussionException
    {
        ChangeRequestReference changeRequestReference = new ChangeRequestReference(originalChangeRequest.getId());
        Map<List<String>, ChangeRequest> changeRequestMap = new HashMap<>();
        for (ChangeRequest splittedChangeRequest : splittedChangeRequests) {
            List<String> fileChangeIdList =
                splittedChangeRequest.getModifiedDocuments().stream()
                    .map(reference -> stringEntityReferenceSerializer.serialize(reference))
                    .collect(Collectors.toList());
            changeRequestMap.put(fileChangeIdList, splittedChangeRequest);
        }

        List<Discussion> allDiscussions = this.getDiscussionsFrom(changeRequestReference);
        for (Discussion discussion : allDiscussions) {
            this.moveDiscussion(discussion, changeRequestMap);
        }
    }

    private void moveDiscussion(Discussion discussion, Map<List<String>, ChangeRequest> splittedChangeRequests)
        throws ChangeRequestDiscussionException
    {
        AbstractChangeRequestDiscussionContextReference reference = this.getReferenceFrom(discussion);
        ChangeRequest newChangeRequest = null;
        FileDiffLocation fileDiffLocation;
        String lostDiscussionLoggerMsg = "Cannot find change request associated with file [{}] the discussion might be "
            + "lost";
        switch (reference.getType()) {
            case FILE_DIFF:
                ChangeRequestFileDiffReference fileDiffReference = (ChangeRequestFileDiffReference) reference;
                fileDiffLocation = fileDiffReference.getFileDiffLocation();
                newChangeRequest = findChangeRequest(splittedChangeRequests, fileDiffLocation);
                if (newChangeRequest != null) {
                    this.moveFileDiffDiscussion(fileDiffReference, discussion, newChangeRequest);
                } else {
                    this.logger.error(lostDiscussionLoggerMsg, fileDiffLocation.getTargetReference());
                }
                break;

            case LINE_DIFF:
                ChangeRequestLineDiffReference lineDiffReference = (ChangeRequestLineDiffReference) reference;
                fileDiffLocation = lineDiffReference.getLineDiffLocation().getFileDiffLocation();
                newChangeRequest = findChangeRequest(splittedChangeRequests, fileDiffLocation);
                if (newChangeRequest != null) {
                    this.moveLineDiffDiscussion(lineDiffReference, discussion, newChangeRequest);
                } else {
                    this.logger.error(lostDiscussionLoggerMsg, fileDiffLocation.getTargetReference());
                }
                break;

            case REVIEW:
            case REVIEWS:
            case CHANGE_REQUEST:
            case CHANGE_REQUEST_COMMENT:
            default:
                this.moveGlobalDiscussion(reference, discussion, splittedChangeRequests.values());
        }
    }

    private ChangeRequest findChangeRequest(Map<List<String>, ChangeRequest> splittedChangeRequests,
        FileDiffLocation fileDiffLocation)
    {
        ChangeRequest result = null;
        for (Map.Entry<List<String>, ChangeRequest> entry : splittedChangeRequests.entrySet()) {
            if (entry.getKey().contains(fileDiffLocation.getTargetReference())) {
                result = entry.getValue();
                break;
            }
        }
        return result;
    }

    private void moveLineDiffDiscussion(ChangeRequestLineDiffReference lineDiffReference, Discussion discussion,
        ChangeRequest newChangeRequest) throws ChangeRequestDiscussionException
    {
        ChangeRequestLineDiffReference newReference = new ChangeRequestLineDiffReference(newChangeRequest.getId(),
            lineDiffReference.getLineDiffLocation()
        );
        Discussion newDiscussion = this.getOrCreateDiscussionFor(newReference);
        this.changeRequestDiscussionFactory.copyMessages(discussion, newDiscussion, newReference);
    }

    private void moveFileDiffDiscussion(ChangeRequestFileDiffReference fileDiffReference, Discussion discussion,
        ChangeRequest newChangeRequest) throws ChangeRequestDiscussionException
    {
        ChangeRequestFileDiffReference newReference = new ChangeRequestFileDiffReference(newChangeRequest.getId(),
            fileDiffReference.getFileDiffLocation());
        Discussion newDiscussion = this.getOrCreateDiscussionFor(newReference);
        this.changeRequestDiscussionFactory.copyMessages(discussion, newDiscussion, newReference);
    }

    private void moveGlobalDiscussion(AbstractChangeRequestDiscussionContextReference reference, Discussion discussion,
        Collection<ChangeRequest> splittedChangeRequests) throws ChangeRequestDiscussionException
    {
        for (ChangeRequest splittedChangeRequest : splittedChangeRequests) {
            AbstractChangeRequestDiscussionContextReference newReference = null;

            if (reference instanceof ChangeRequestReference) {
                newReference = new ChangeRequestReference(splittedChangeRequest.getId());
            } else if (reference instanceof ChangeRequestCommentReference) {
                newReference = new ChangeRequestCommentReference(splittedChangeRequest.getId());
            } else if (reference instanceof ChangeRequestReviewsReference) {
                newReference = new ChangeRequestReviewsReference(splittedChangeRequest.getId());
            } else if (reference instanceof ChangeRequestReviewReference) {
                ChangeRequestReviewReference reviewReference = (ChangeRequestReviewReference) reference;
                newReference = new ChangeRequestReviewReference(reviewReference.getReviewId(),
                    splittedChangeRequest.getId());
            }
            Discussion newDiscussion = this.getOrCreateDiscussionFor(newReference);
            this.changeRequestDiscussionFactory.copyMessages(discussion, newDiscussion, newReference);
        }
    }

    @Override
    public <T extends AbstractChangeRequestDiscussionContextReference> DiscussionContext
        getOrCreateDiscussionContextFor(T reference) throws ChangeRequestDiscussionException
    {
        return this.changeRequestDiscussionFactory.getOrCreateContextFor(reference);
    }

    @Override
    public boolean attachDiffBlockMetadata(DiscussionReference discussionReference,
        UnifiedDiffBlock<String, Character> contextBlock) throws ChangeRequestDiscussionException
    {
        boolean result = false;
        Optional<Discussion> discussionOptional = this.discussionService.get(discussionReference);
        if (discussionOptional.isPresent()) {
            AbstractChangeRequestDiscussionContextReference reference = this.getReferenceFrom(discussionOptional.get());
            if (reference instanceof ChangeRequestLineDiffReference) {
                DiscussionContext discussionContext =
                    this.changeRequestDiscussionFactory.getOrCreateContextFor(reference);

                try {
                    this.discussionContextService.saveMetadata(discussionContext,
                        Collections.singletonMap(DIFF_CONTEXT_METADATA_KEY,
                            this.changeRequestDiscussionDiffUtils.serialize(contextBlock)));
                } catch (JsonProcessingException e) {
                    throw new ChangeRequestDiscussionException("Error when trying to serialize the context block", e);
                }
                result = true;
            } else {
                logger.error("Trying to attach a diff context to a reference not of type line diff: [{}]", reference);
            }
        }

        return result;
    }

    @Override
    public Optional<ChangeRequestDiscussionDiffBlock> getDiffBlockMetadata(Discussion discussion)
        throws ChangeRequestException
    {
        Optional<ChangeRequestDiscussionDiffBlock> result = Optional.empty();
        List<DiscussionContext> discussionContexts =
            this.discussionContextService.findByDiscussionReference(discussion.getReference());
        AbstractChangeRequestDiscussionContextReference referenceFrom = getReferenceFrom(discussion);
        if (referenceFrom instanceof ChangeRequestLineDiffReference) {
            ChangeRequestLineDiffReference lineDiffReference = (ChangeRequestLineDiffReference) referenceFrom;
            for (DiscussionContext discussionContext : discussionContexts) {
                Map<String, String> metadata = discussionContext.getMetadata();
                if (metadata.containsKey(DIFF_CONTEXT_METADATA_KEY)) {
                    String jsonDiffBlockSerialization = metadata.get(DIFF_CONTEXT_METADATA_KEY);
                    try {
                        UnifiedDiffBlock<String, Character> unifiedDiffBlock =
                            this.changeRequestDiscussionDiffUtils.deserialize(jsonDiffBlockSerialization);
                        result = Optional.of(new ChangeRequestDiscussionDiffBlock(unifiedDiffBlock, lineDiffReference));
                    } catch (JsonProcessingException e) {
                        throw new ChangeRequestException("Error when parsing json serialization of diff block", e);
                    }
                    break;
                }
            }
        }

        return result;
    }
}
