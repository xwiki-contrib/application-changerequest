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
package org.xwiki.contrib.changerequest.script;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ApproversManager;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestManager;
import org.xwiki.contrib.changerequest.ChangeRequestMergeDocumentResult;
import org.xwiki.contrib.changerequest.ChangeRequestReference;
import org.xwiki.contrib.changerequest.ChangeRequestReview;
import org.xwiki.contrib.changerequest.ChangeRequestStatus;
import org.xwiki.contrib.changerequest.ConflictResolutionChoice;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.MergeApprovalStrategy;
import org.xwiki.contrib.changerequest.events.ChangeRequestReviewAddedEvent;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.contrib.changerequest.storage.ReviewStorageManager;
import org.xwiki.diff.Conflict;
import org.xwiki.diff.ConflictDecision;
import org.xwiki.diff.internal.DefaultConflictDecision;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.observation.ObservationManager;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.resource.SerializeResourceReferenceException;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.script.service.ScriptService;
import org.xwiki.script.service.ScriptServiceManager;
import org.xwiki.stability.Unstable;
import org.xwiki.store.merge.MergeDocumentResult;
import org.xwiki.url.ExtendedURL;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

/**
 * Script service for change request.
 *
 * @version $Id$
 * @since 0.1
 */
@Unstable
@Component
@Named("changerequest")
@Singleton
public class ChangeRequestScriptService implements ScriptService
{
    @Inject
    private ChangeRequestManager changeRequestManager;

    @Inject
    private ChangeRequestStorageManager changeRequestStorageManager;

    @Inject
    private UserReferenceResolver<CurrentUserReference> currentUserReferenceResolver;

    @Inject
    private ResourceReferenceSerializer<ChangeRequestReference, ExtendedURL> urlResourceReferenceSerializer;

    @Inject
    private DocumentReferenceResolver<ChangeRequest> documentReferenceResolver;

    @Inject
    private ReviewStorageManager reviewStorageManager;

    @Inject
    private ApproversManager<ChangeRequest> changeRequestApproversManager;

    @Inject
    private ObservationManager observationManager;

    @Inject
    private Logger logger;

    @Inject
    private ScriptServiceManager scriptServiceManager;

    /**
     * @param <S> the type of the {@link ScriptService}
     * @param serviceName the name of the sub {@link ScriptService}
     * @return the {@link ScriptService} or null of none could be found
     */
    @SuppressWarnings("unchecked")
    public <S extends ScriptService> S get(String serviceName)
    {
        return (S) this.scriptServiceManager.get("changerequest." + serviceName);
    }

    /**
     * Retrieve the change request identified with the given id.
     *
     * @param changeRequestId the identifier of a change request.
     * @return an optional containing the change request instance if it can be found, else an empty optional.
     */
    public Optional<ChangeRequest> getChangeRequest(String changeRequestId) throws ChangeRequestException
    {
        return this.changeRequestStorageManager.load(changeRequestId);
    }

    /**
     * Check if the current user is authorized to merge the given changed request.
     *
     * @param changeRequest the change request to be checked for merging authorization.
     * @return {@code true} if the current user has proper rights to merge the given change request.
     * @since 0.3
     */
    @Unstable
    public boolean isAuthorizedToMerge(ChangeRequest changeRequest) throws ChangeRequestException
    {
        UserReference currentUser = this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE);
        return this.changeRequestManager.isAuthorizedToMerge(currentUser, changeRequest);
    }

    /**
     * Check if the given change request can be merged.
     * This method checks if the approval strategy is reached and if the change request has conflicts.
     *
     * @param changeRequest the change request to check.
     * @return {@code true} if the given change request can be merged (i.e. the approval strategy
     *          allows it and the change request does not have conflicts).
     */
    public boolean canBeMerged(ChangeRequest changeRequest) throws ChangeRequestException
    {
        return this.changeRequestManager.canBeMerged(changeRequest);
    }

    /**
     * Retrieve the modified document containing in the given change request and identified by the given reference.
     *
     * @param changeRequest the change request where to find the modified document
     * @param documentReference the reference of the modified document to get
     * @return an optional containing the instance of modified document or an empty optional if it cannot be found.
     */
    public Optional<DocumentModelBridge> getModifiedDocument(ChangeRequest changeRequest,
        DocumentReference documentReference)
    {
        Optional<FileChange> latestFileChange = changeRequest.getLatestFileChangeFor(documentReference);
        return latestFileChange.map(FileChange::getModifiedDocument);
    }

    /**
     * Retrieve all change requests that contain a change for the given document.
     *
     * @param documentReference the reference to look for in the change requests.
     * @return the list of all change requests containing a change for the given document.
     * @since 0.3
     */
    public List<ChangeRequest> getChangeRequestWithChangesFor(DocumentReference documentReference)
        throws ChangeRequestException
    {
        return this.changeRequestStorageManager.findChangeRequestTargeting(documentReference);
    }

    /**
     * Retrieve change requests different from the given change request, that are not closed or merged and which
     * contains a change for one of the document reference modified by the given change request.
     *
     * @param changeRequest the change request from which to take the modified documents.
     * @return a map whose keys are the given document references and values the list of found change requests. If no
     *          change request is found for a given reference, the entry is not added.
     * @since 0.7
     */
    public Map<DocumentReference, List<ChangeRequest>> getOpenChangeRequestsTargetingSame(ChangeRequest changeRequest)
        throws ChangeRequestException
    {
        Map<DocumentReference, List<ChangeRequest>> result = new HashMap<>();

        for (DocumentReference modifiedDocument : changeRequest.getModifiedDocuments()) {
            List<ChangeRequest> changeRequests = getChangeRequestWithChangesFor(modifiedDocument).stream()
                .filter(foundCR ->
                    !foundCR.getId().equals(changeRequest.getId())
                    && foundCR.getStatus() != ChangeRequestStatus.CLOSED
                    && foundCR.getStatus() != ChangeRequestStatus.MERGED
            ).collect(Collectors.toList());
            if (!changeRequests.isEmpty()) {
                result.put(modifiedDocument, changeRequests);
            }
        }

        return result;
    }

    /**
     * Find all change request documents whose title is matching the given title.
     *
     * @param title a partial title for finding the change requests.
     * @return a list of document references corresponding to change request pages.
     * @since 0.3
     */
    public List<DocumentReference> findChangeRequestMatchingTitle(String title) throws ChangeRequestException
    {
        return this.changeRequestStorageManager.getChangeRequestMatchingName(title);
    }

    /**
     * Retrieve the URL for the given change request action and id.
     * @param action the change request action as defined by
     *          {@link org.xwiki.contrib.changerequest.ChangeRequestReference.ChangeRequestAction}.
     * @param changeRequestId the change request id.
     * @return an URL as a String or an empty string in case of error.
     * @since 0.3
     */
    public String getChangeRequestURL(String action, String changeRequestId)
        throws SerializeResourceReferenceException, UnsupportedResourceReferenceException
    {
        ChangeRequestReference.ChangeRequestAction requestAction =
            ChangeRequestReference.ChangeRequestAction.valueOf(action.toUpperCase(Locale.ROOT));
        ChangeRequestReference reference = new ChangeRequestReference(requestAction, changeRequestId);
        ExtendedURL extendedURL = this.urlResourceReferenceSerializer.serialize(reference);
        return extendedURL.serialize();
    }

    /**
     * Compute the reference of the actual document which stores the change request.
     *
     * @param changeRequest the change request for which to get the store document reference.
     * @return a document reference where the change request can be seen.
     * @since 0.4
     */
    public DocumentReference getChangeRequestDocumentReference(ChangeRequest changeRequest)
    {
        return this.documentReferenceResolver.resolve(changeRequest);
    }

    /**
     * Check if the current user can change the status of the change request.
     *
     * @param changeRequest the change request for which to check the authors.
     * @return {@code true} if the current user is one of the author of the given change request and the change request
     *          is not merged yet.
     * @since 0.4
     */
    public boolean canStatusBeChanged(ChangeRequest changeRequest)
    {
        UserReference currentUser = this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE);
        return this.changeRequestManager.isAuthorizedToChangeStatus(currentUser, changeRequest);
    }

    /**
     * Mark the given change request as ready for review.
     *
     * @param changeRequest the change request for which to change the status.
     * @since 0.4
     */
    public void setReadyForReview(ChangeRequest changeRequest) throws ChangeRequestException
    {
        this.changeRequestManager.updateStatus(changeRequest, ChangeRequestStatus.READY_FOR_REVIEW);
    }

    /**
     * Mark the given change request as draft.
     *
     * @param changeRequest the change request for which to change the status.
     * @since 0.4
     */
    public void setDraft(ChangeRequest changeRequest) throws ChangeRequestException
    {
        this.changeRequestManager.updateStatus(changeRequest, ChangeRequestStatus.DRAFT);
    }

    /**
     * Mark the given change request as closed.
     *
     * @param changeRequest the change request for which to change the status.
     * @throws ChangeRequestException in case of problem when saving the status.
     *
     * @since 0.6
     */
    public void setClose(ChangeRequest changeRequest) throws ChangeRequestException
    {
        this.changeRequestManager.updateStatus(changeRequest, ChangeRequestStatus.CLOSED);
    }

    /**
     * Perform a merge without saving between the changes of the change request related to the given reference, and the
     * published document with same reference, and returns the merge result.
     *
     * @param changeRequest the change request for which to find changes.
     * @param documentReference the document reference for which to perform a merge.
     * @return a {@link Optional#empty()} if no change for the given reference can be found or if an error occurs
     *         during the merge, else an optional containing the {@link MergeDocumentResult}.
     * @since 0.4
     */
    public Optional<ChangeRequestMergeDocumentResult> getMergeDocumentResult(ChangeRequest changeRequest,
        DocumentReference documentReference) throws ChangeRequestException
    {
        Optional<ChangeRequestMergeDocumentResult> result = Optional.empty();
        Optional<FileChange> optionalFileChange = changeRequest.getLatestFileChangeFor(documentReference);
        if (optionalFileChange.isPresent()) {
            ChangeRequestMergeDocumentResult changeRequestMergeDocumentResult =
                this.changeRequestManager.getMergeDocumentResult(optionalFileChange.get());
            result = Optional.of(changeRequestMergeDocumentResult);
        }
        return result;
    }

    /**
     * Allow to create a {@link ConflictDecision} based on the given parameters.
     *
     * @param mergeDocumentResult the merge result for which to create a conflict decision.
     * @param conflictReference the reference of the conflict for which to create the decision.
     * @param decisionType the decision taken for fixing the conflict.
     * @param customResolution a custom resolution input. Note that if this parameter is given, then the decisionType
     *                         will be set to custom.
     * @return an {@link Optional#empty()} if no conflict matches the given reference in the merge result, else returns
     *          a {@link ConflictDecision} with the appropriate information to be used in
     *          {@link #fixConflicts(ChangeRequest, DocumentReference, ConflictResolutionChoice, List)}.
     * @since 0.4
     */
    public Optional<ConflictDecision<?>> createConflictDecision(MergeDocumentResult mergeDocumentResult,
        String conflictReference, ConflictDecision.DecisionType decisionType, List<Object> customResolution)
    {
        Optional<ConflictDecision<?>> result = Optional.empty();
        Conflict<?> concernedConflict = null;
        for (Conflict<?> conflict : mergeDocumentResult.getConflicts()) {
            if (StringUtils.equals(conflictReference, conflict.getReference())) {
                concernedConflict = conflict;
                break;
            }
        }
        if (concernedConflict != null) {
            ConflictDecision<Object> decision = new DefaultConflictDecision<>(concernedConflict);
            decision.setType(decisionType);
            if (customResolution != null && !customResolution.isEmpty()) {
                decision.setCustom(customResolution);
            }
            result = Optional.of(decision);
        }

        return result;
    }

    /**
     * Fix conflicts related to the given {@link MergeDocumentResult} by applying the given decision.
     *
     * @param changeRequest the change request for which to fix the conflicts.
     * @param documentReference the document reference for which to perform a merge.
     * @param resolutionChoice the global choice to make.
     * @param customDecisions the specific decisions in case the resolution choice was
     *          {@link ConflictResolutionChoice#CUSTOM}.
     * @return {@code true} if the conflicts were properly fixed, {@code false} if any problem occurs preventing to fix
     *          the conflict.
     * @since 0.4
     */
    public boolean fixConflicts(ChangeRequest changeRequest, DocumentReference documentReference,
        ConflictResolutionChoice resolutionChoice, List<ConflictDecision<?>> customDecisions)
        throws ChangeRequestException
    {
        boolean result = false;
        if (this.canFixConflict(changeRequest, documentReference)) {
            Optional<FileChange> optionalFileChange = changeRequest.getLatestFileChangeFor(documentReference);
            if (optionalFileChange.isPresent()) {
                result = this.changeRequestManager
                    .mergeWithConflictDecision(optionalFileChange.get(), resolutionChoice, customDecisions);
            }
        }

        return result;
    }

    /**
     * Allow to add a review to the given change request.
     *
     * @param changeRequest the change request for which to add a review.
     * @param approved {@code true} if the review is an approval, {@code false} if it requests changes.
     * @param comment an optional comment with the review.
     * @return {@code true} if the review has been properly stored.
     * @since 0.4
     */
    public boolean addReview(ChangeRequest changeRequest, boolean approved, String comment)
        throws ChangeRequestException
    {
        boolean result = false;
        UserReference userReference = this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE);
        if (this.isAuthorizedToReview(changeRequest)) {
            ChangeRequestReview review = new ChangeRequestReview(changeRequest, approved, userReference);
            review.setComment(comment);
            this.reviewStorageManager.save(review);
            changeRequest.addReview(review);
            this.observationManager.notify(new ChangeRequestReviewAddedEvent(), changeRequest.getId(), review);
            this.changeRequestManager.computeReadyForMergingStatus(changeRequest);
            result = true;
        } else {
            logger.warn("Unauthorized user [{}] trying to add review to [{}].", userReference, changeRequest);
        }
        return result;
    }

    /**
     * Retrieve the {@link MergeApprovalStrategy} to be used.
     *
     * @return an {@link Optional#empty()} in case of problem to get the strategy, else an optional containing the
     * {@link MergeApprovalStrategy}.
     * @since 0.4
     */
    public MergeApprovalStrategy getMergeApprovalStrategy() throws ChangeRequestException
    {
        return this.changeRequestManager.getMergeApprovalStrategy();
    }

    /**
     * Check if the current user is authorized to review the given change request.
     *
     * @param changeRequest the change request about to be reviewed.
     * @return {@code true} if the change request can be reviewed by current user.
     * @since 0.4
     */
    public boolean isAuthorizedToReview(ChangeRequest changeRequest) throws ChangeRequestException
    {
        UserReference currentUserReference = this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE);
        return this.changeRequestManager.isAuthorizedToReview(currentUserReference, changeRequest);
    }

    /**
     * Check if the current user is authorized to rebase the given change request.
     *
     * @param changeRequest the change request about to be rebased.
     * @return {@code true} if the change request can be rebased by current user.
     * @since 0.7
     */
    public boolean isAuthorizedToRebase(ChangeRequest changeRequest) throws ChangeRequestException
    {
        UserReference currentUserReference = this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE);
        return this.changeRequestManager.isAuthorizedToRebase(currentUserReference, changeRequest);
    }

    /**
     * Retrieve a review based on its identifier.
     *
     * @param changeRequest the change request for which to retrieve a review.
     * @param reviewId the id of the review to retrieve.
     * @return an {@link Optional#empty()} if the review cannot be found, else an optional containing the requested
     *          {@link ChangeRequestReview}.
     * @since 0.4
     */
    public Optional<ChangeRequestReview> getReview(ChangeRequest changeRequest, String reviewId)
    {
        return changeRequest.getReviews().stream().filter(review -> reviewId.equals(review.getId())).findFirst();
    }

    /**
     * Change the validity of the review to mark it as invalid, or on contrary to make it valid again.
     *
     * @param review the review for which to change the validity status.
     * @param isValid the new validity status to set.
     * @return {@code true} if the review has been properly saved with the new status.
     * @since 0.4
     */
    public boolean setReviewValidity(ChangeRequestReview review, boolean isValid) throws ChangeRequestException
    {
        if (canEditReview(review)) {
            review.setValid(isValid);
            review.setSaved(false);
            this.reviewStorageManager.save(review);
            this.changeRequestManager.computeReadyForMergingStatus(review.getChangeRequest());
            return true;
        }
        return false;
    }

    /**
     * Check if the current user can edit the given review.
     * @param review the review for which to check if it can be edited.
     * @return {@code true} if the review is authored by the current user.
     * @since 0.4
     */
    public boolean canEditReview(ChangeRequestReview review)
    {
        UserReference currentUserReference = this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE);
        return review.getAuthor().equals(currentUserReference);
    }

    /**
     * Check if the current user can fix a conflict related to the given document reference in the given change request.
     * @param changeRequest the change request concerned by a conflict.
     * @param documentReference the reference of the document concerned by the conflict.
     * @return {@code true} if the current user is authorized to fix the conflict.
     * @since 0.4
     */
    public boolean canFixConflict(ChangeRequest changeRequest, DocumentReference documentReference)
    {
        UserReference currentUserReference = this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE);
        Optional<FileChange> optionalFileChange = changeRequest.getLatestFileChangeFor(documentReference);
        if (optionalFileChange.isPresent()) {
            return this.changeRequestManager.isAuthorizedToFixConflict(currentUserReference, optionalFileChange.get());
        }
        return false;
    }

    /**
     * Check if the given document can be requested for deletion.
     *
     * @param documentReference the document for which to check if it can be requested for deletion.
     * @return {@code true} if the document can be requested for deletion.
     * @since 0.5
     */
    public boolean canDeletionBeRequested(DocumentReference documentReference) throws ChangeRequestException
    {
        return this.changeRequestManager.canDeletionBeRequested(documentReference);
    }

    /**
     * Retrieve all explicit approvers for the given change request.
     *
     * @param changeRequest the request for which to get approvers.
     * @return the list of approvers.
     * @since 0.5
     */
    public Set<UserReference> getApprovers(ChangeRequest changeRequest) throws ChangeRequestException
    {
        return this.changeRequestApproversManager.getAllApprovers(changeRequest, true);
    }

    /**
     * Retrieve a file change based on its id.
     * @param changeRequest the change request from where to retrieve the file change.
     * @param fileChangeId the id of the file change to find.
     * @return an {@link Optional#empty()} if the file change cannot be retrieved, else the retrieved file change.
     * @since 0.6
     */
    public Optional<FileChange> getFileChange(ChangeRequest changeRequest, String fileChangeId)
    {
        return changeRequest.getAllFileChanges().stream()
            .filter(fileChange -> StringUtils.equals(fileChangeId, fileChange.getId()))
            .findFirst();
    }
}
