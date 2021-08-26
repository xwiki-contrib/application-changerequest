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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestManager;
import org.xwiki.contrib.changerequest.ChangeRequestReference;
import org.xwiki.contrib.changerequest.ChangeRequestReview;
import org.xwiki.contrib.changerequest.ChangeRequestStatus;
import org.xwiki.contrib.changerequest.ConflictResolutionChoice;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.MergeApprovalStrategy;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.contrib.changerequest.storage.ReviewStorageManager;
import org.xwiki.diff.Conflict;
import org.xwiki.diff.ConflictDecision;
import org.xwiki.diff.internal.DefaultConflictDecision;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.resource.SerializeResourceReferenceException;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.script.service.ScriptService;
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
    private Logger logger;

    /**
     * Retrieve the change request identified with the given id.
     *
     * @param changeRequestId the identifier of a change request.
     * @return an optional containing the change request instance if it can be found, else an empty optional.
     */
    public Optional<ChangeRequest> getChangeRequest(String changeRequestId)
    {
        try {
            return this.changeRequestStorageManager.load(changeRequestId);
        } catch (ChangeRequestException e) {
            this.logger.warn("Error while loading change request with id [{}]", changeRequestId, e);
        }
        return Optional.empty();
    }

    /**
     * Check if the current user is authorized to merge the given changed request.
     *
     * @param changeRequest the change request to be checked for merging authorization.
     * @return {@code true} if the current user has proper rights to merge the given change request.
     * @since 0.3
     */
    @Unstable
    public boolean isAuthorizedToMerge(ChangeRequest changeRequest)
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
    public boolean canBeMerged(ChangeRequest changeRequest)
    {
        boolean result = false;
        try {
            result = this.changeRequestManager.canBeMerged(changeRequest);
        } catch (ChangeRequestException e) {
            this.logger.warn("Error while checking if the change request [{}] can be merged: [{}]",
                changeRequest, ExceptionUtils.getRootCauseMessage(e));
        }
        return result;
    }

    /**
     * Retrieve all document references that have been impacted by a change in that change request.
     *
     * @param changeRequest the change request for which to get the changed documents.
     * @return a list of document references.
     */
    public List<DocumentReference> getChangedDocuments(ChangeRequest changeRequest)
    {
        return new ArrayList<>(changeRequest.getFileChanges().keySet());
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
        if (changeRequest.getFileChanges().containsKey(documentReference)) {
            return Optional.of(changeRequest.getFileChanges().get(documentReference).peekLast().getModifiedDocument());
        }
        return Optional.empty();
    }

    /**
     * Retrieve all change requests that contain a change for the given document.
     *
     * @param documentReference the reference to look for in the change requests.
     * @return the list of all change requests containing a change for the given document.
     * @since 0.3
     */
    public List<ChangeRequest> getChangeRequestWithChangesFor(DocumentReference documentReference)
    {
        try {
            return this.changeRequestStorageManager.findChangeRequestTargeting(documentReference);
        } catch (ChangeRequestException e) {
            logger.warn("Error while getting change requests for document [{}]: [{}]", documentReference,
                ExceptionUtils.getRootCauseMessage(e));
        }
        return Collections.emptyList();
    }

    /**
     * Find all change request documents whose title is matching the given title.
     *
     * @param title a partial title for finding the change requests.
     * @return a list of document references corresponding to change request pages.
     * @since 0.3
     */
    public List<DocumentReference> findChangeRequestMatchingTitle(String title)
    {
        try {
            return this.changeRequestStorageManager.getChangeRequestMatchingName(title);
        } catch (ChangeRequestException e) {
            logger.warn("Error while getting change requests for title [{}]: [{}]", title,
                ExceptionUtils.getRootCauseMessage(e));
        }
        return Collections.emptyList();
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
    {
        ChangeRequestReference.ChangeRequestAction requestAction =
            ChangeRequestReference.ChangeRequestAction.valueOf(action.toUpperCase(Locale.ROOT));
        ChangeRequestReference reference = new ChangeRequestReference(requestAction, changeRequestId);
        try {
            ExtendedURL extendedURL = this.urlResourceReferenceSerializer.serialize(reference);
            return extendedURL.serialize();
        } catch (SerializeResourceReferenceException | UnsupportedResourceReferenceException e) {
            logger.warn("Error while serializing URL for reference [{}]: [{}]", reference,
                ExceptionUtils.getRootCauseMessage(e));
        }
        return "";
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
        return changeRequest.getStatus() != ChangeRequestStatus.MERGED
            && changeRequest.getAuthors().contains(currentUser);
    }

    private void setStatus(ChangeRequest changeRequest, ChangeRequestStatus status)
    {
        if (changeRequest.getStatus() != status) {
            changeRequest.setStatus(status);
            try {
                this.changeRequestStorageManager.save(changeRequest);
            } catch (ChangeRequestException e) {
                logger.warn("Error while saving the change request: [{}]", ExceptionUtils.getRootCauseMessage(e));
            }
        }
    }

    /**
     * Mark the given change request as ready for review.
     *
     * @param changeRequest the change request for which to change the status.
     * @since 0.4
     */
    public void setReadyForReview(ChangeRequest changeRequest)
    {
        setStatus(changeRequest, ChangeRequestStatus.READY_FOR_REVIEW);
    }

    /**
     * Mark the given change request as draft.
     *
     * @param changeRequest the change request for which to change the status.
     * @since 0.4
     */
    public void setDraft(ChangeRequest changeRequest)
    {
        setStatus(changeRequest, ChangeRequestStatus.DRAFT);
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
    public Optional<MergeDocumentResult> getMergeDocumentResult(ChangeRequest changeRequest,
        DocumentReference documentReference)
    {
        Optional<MergeDocumentResult> result = Optional.empty();
        Optional<FileChange> optionalFileChange = changeRequest.getLatestFileChangeFor(documentReference);
        if (optionalFileChange.isPresent()) {
            try {
                MergeDocumentResult mergeDocumentResult =
                    this.changeRequestManager.getMergeDocumentResult(optionalFileChange.get());
                result = Optional.of(mergeDocumentResult);
            } catch (ChangeRequestException e) {
                logger.warn("Error while computing the merge for change request [{}] and document reference [{}]: [{}]",
                    changeRequest.getId(), documentReference, ExceptionUtils.getRootCauseMessage(e));
            }
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
    {
        boolean result = false;
        if (this.canFixConflict(changeRequest, documentReference)) {
            Optional<FileChange> optionalFileChange = changeRequest.getLatestFileChangeFor(documentReference);
            if (optionalFileChange.isPresent()) {
                try {
                    result = this.changeRequestManager
                        .mergeWithConflictDecision(optionalFileChange.get(), resolutionChoice, customDecisions);
                } catch (ChangeRequestException e) {
                    logger.warn("Error while trying to fix conflicts for [{}] in [{}] with decision [{}]: [{}]",
                        documentReference, changeRequest.getId(), resolutionChoice,
                        ExceptionUtils.getRootCauseMessage(e));
                }
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
    {
        boolean result = false;
        UserReference userReference = this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE);
        if (this.isAuthorizedToReview(changeRequest)) {
            ChangeRequestReview review = new ChangeRequestReview(changeRequest, approved, userReference);
            review.setComment(comment);
            try {
                this.reviewStorageManager.save(review);
                result = true;
            } catch (ChangeRequestException e) {
                logger.warn("Error while saving review for change request [{}]: [{}]",
                    changeRequest.getId(), ExceptionUtils.getRootCauseMessage(e));
            }
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
    public Optional<MergeApprovalStrategy> getMergeApprovalStrategy()
    {
        try {
            return Optional.of(this.changeRequestManager.getMergeApprovalStrategy());
        } catch (ChangeRequestException e) {
            logger.warn("Error while getting merge approval strategy: [{}]", ExceptionUtils.getRootCauseMessage(e));
        }
        return Optional.empty();
    }

    /**
     * Check if the current user is authorized to review the given change request.
     *
     * @param changeRequest the change request about to be reviewed.
     * @return {@code true} if the change request can be reviewed by current user.
     * @since 0.4
     */
    public boolean isAuthorizedToReview(ChangeRequest changeRequest)
    {
        UserReference currentUserReference = this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE);
        return this.changeRequestManager.isAuthorizedToReview(currentUserReference, changeRequest);
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
    public boolean setReviewValidity(ChangeRequestReview review, boolean isValid)
    {
        if (canEditReview(review)) {
            review.setValid(isValid);
            review.setSaved(false);
            try {
                this.reviewStorageManager.save(review);
                return true;
            } catch (ChangeRequestException e) {
                logger.warn("Error while saving the review [{}]: [{}]", review, ExceptionUtils.getRootCauseMessage(e));
            }
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
}
