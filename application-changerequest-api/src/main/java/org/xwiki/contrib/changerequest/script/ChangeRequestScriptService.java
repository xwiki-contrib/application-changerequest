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

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestManager;
import org.xwiki.contrib.changerequest.ChangeRequestReference;
import org.xwiki.contrib.changerequest.ChangeRequestStatus;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
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
}
