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
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.changerequest.ApproversManager;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestConfiguration;
import org.xwiki.contrib.changerequest.ChangeRequestDiffManager;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestManager;
import org.xwiki.contrib.changerequest.ChangeRequestReference;
import org.xwiki.contrib.changerequest.ChangeRequestStatus;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.FileChangeSavingChecker;
import org.xwiki.contrib.changerequest.MergeApprovalStrategy;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.resource.SerializeResourceReferenceException;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.script.service.ScriptService;
import org.xwiki.script.service.ScriptServiceManager;
import org.xwiki.stability.Unstable;
import org.xwiki.url.ExtendedURL;
import org.xwiki.user.UserReference;

import com.xpn.xwiki.XWikiContext;

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
    private ResourceReferenceSerializer<ChangeRequestReference, ExtendedURL> urlResourceReferenceSerializer;

    @Inject
    private DocumentReferenceResolver<ChangeRequest> documentReferenceResolver;

    @Inject
    private ApproversManager<ChangeRequest> changeRequestApproversManager;

    @Inject
    private ScriptServiceManager scriptServiceManager;

    @Inject
    private ChangeRequestConfiguration configuration;

    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private ApproversManager<DocumentReference> documentReferenceApproversManager;

    @Inject
    private ChangeRequestDiffManager diffManager;

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
     * @throws ChangeRequestException in case of problem when retrieving the change request.
     */
    public Optional<ChangeRequest> getChangeRequest(String changeRequestId) throws ChangeRequestException
    {
        return this.changeRequestStorageManager.load(changeRequestId);
    }

    /**
     * Check if the given change request can be merged.
     * This method checks if the approval strategy is reached and if the change request has conflicts.
     *
     * @param changeRequest the change request to check.
     * @return {@code true} if the given change request can be merged (i.e. the approval strategy
     *          allows it and the change request does not have conflicts).
     * @throws ChangeRequestException in case of problem when loading information.
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
     * @throws ChangeRequestException in case of problem when loading change request.
     * @since 0.3
     */
    public List<ChangeRequest> getChangeRequestWithChangesFor(DocumentReference documentReference)
        throws ChangeRequestException
    {
        return this.changeRequestStorageManager.findChangeRequestTargeting(documentReference);
    }

    /**
     * Retrieve all open change requests that contain a change for the given document.
     *
     * @param documentReference the reference to look for in the change requests.
     * @return the list of all open change requests containing a change for the given document.
     * @throws ChangeRequestException in case of problem when loading change request.
     * @since 0.11
     */
    public List<ChangeRequest> getOpenChangeRequestWithChangesFor(DocumentReference documentReference)
        throws ChangeRequestException
    {
        List<ChangeRequest> changeRequestTargeting =
            this.changeRequestStorageManager.findChangeRequestTargeting(documentReference);
        return changeRequestTargeting.stream().filter(cr -> cr.getStatus().isOpen()).collect(Collectors.toList());
    }

    /**
     * Retrieve change requests different from the given change request, that are not closed or merged and which
     * contains a change for one of the document reference modified by the given change request.
     *
     * @param changeRequest the change request from which to take the modified documents.
     * @return a map whose keys are the given document references and values the list of found change requests. If no
     *          change request is found for a given reference, the entry is not added.
     * @throws ChangeRequestException in case of problem for loading other change requests.
     * @since 0.7
     */
    public Map<DocumentReference, List<ChangeRequest>> getOpenChangeRequestsTargetingSame(ChangeRequest changeRequest)
        throws ChangeRequestException
    {
        Map<DocumentReference, List<ChangeRequest>> result = new HashMap<>();

        for (DocumentReference modifiedDocument : changeRequest.getModifiedDocuments()) {
            List<ChangeRequest> changeRequests = getOpenChangeRequestWithChangesFor(modifiedDocument)
                .stream()
                .filter(foundCR -> !foundCR.getId().equals(changeRequest.getId()))
                .collect(Collectors.toList());
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
     * @throws ChangeRequestException in case of problem for loading other change request.
     * @since 0.3
     */
    public List<DocumentReference> findOpenChangeRequestMatchingTitle(String title) throws ChangeRequestException
    {
        return this.changeRequestStorageManager.getOpenChangeRequestMatchingName(title);
    }

    /**
     * Check if the given change identified by the document and the change type can be added to the given change
     * request, or if there's an incompatibility. If there is an incompatibility, this method will return the reason
     * of the incompatibility.
     *
     * @param changeRequestId the identifier of the change request.
     * @param newDocumentChange the reference of the document to add in the change request.
     * @param changeType the type of change to be made.
     * @return a {@link Pair} whose left value is a boolean representing the compatibility, and left value is an empty
     *         string in case of compatibility, or the actual incompatibility reason.
     * @throws ComponentLookupException in case of problem to load the compatibility checkers components
     * @throws ChangeRequestException in case of problem to load the change request
     * @since 0.14
     */
    public FileChangeSavingChecker.SavingCheckerResult checkDocumentChangeCompatibility(String changeRequestId,
        DocumentReference newDocumentChange, FileChange.FileChangeType changeType)
        throws ComponentLookupException, ChangeRequestException
    {
        FileChangeSavingChecker.SavingCheckerResult result = new FileChangeSavingChecker.SavingCheckerResult();

        Optional<ChangeRequest> changeRequestOptional =
            this.changeRequestStorageManager.load(changeRequestId);
        if (changeRequestOptional.isEmpty()) {
            result = new FileChangeSavingChecker.SavingCheckerResult(
                "changerequest.script.compatibility.changeRequestNotFound");
        } else {
            ChangeRequest changeRequest = changeRequestOptional.get();
            List<FileChangeSavingChecker> checkerList =
                this.componentManager.getInstanceList(FileChangeSavingChecker.class);

            for (FileChangeSavingChecker fileChangeSavingChecker : checkerList) {
                result = fileChangeSavingChecker.canChangeOnDocumentBeAdded(changeRequest, newDocumentChange,
                    changeType);
                if (!result.canBeSaved()) {
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Retrieve the URL for the given change request action and id.
     * @param action the change request action as defined by
     *          {@link org.xwiki.contrib.changerequest.ChangeRequestReference.ChangeRequestAction}.
     * @param changeRequestId the change request id.
     * @return an URL as a String or an empty string in case of error.
     * @throws SerializeResourceReferenceException in case of problem for serializing the URL
     * @throws UnsupportedResourceReferenceException if the action is not recognized.
     * @since 0.3
     */
    public String getChangeRequestURL(String action, String changeRequestId)
        throws SerializeResourceReferenceException, UnsupportedResourceReferenceException
    {
        WikiReference wikiReference = this.contextProvider.get().getWikiReference();
        ChangeRequestReference.ChangeRequestAction requestAction =
            ChangeRequestReference.ChangeRequestAction.valueOf(action.toUpperCase(Locale.ROOT));
        ChangeRequestReference reference = new ChangeRequestReference(wikiReference, requestAction, changeRequestId);
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
     * Mark the given change request as ready for review.
     *
     * @param changeRequest the change request for which to change the status.
     * @throws ChangeRequestException in case of problem for saving the change request.
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
     * @throws ChangeRequestException in case of problem for saving the change request.
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
     * Retrieve the {@link MergeApprovalStrategy} to be used.
     *
     * @return an {@link Optional#empty()} in case of problem to get the strategy, else an optional containing the
     * {@link MergeApprovalStrategy}.
     * @throws ChangeRequestException in case of problem for loading the strategy.
     * @since 0.4
     */
    public MergeApprovalStrategy getMergeApprovalStrategy() throws ChangeRequestException
    {
        return this.changeRequestManager.getMergeApprovalStrategy();
    }

    /**
     * Check if the given document can be requested for deletion.
     *
     * @param documentReference the document for which to check if it can be requested for deletion.
     * @return {@code true} if the document can be requested for deletion.
     * @throws ChangeRequestException in case of problem for loading rights.
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
     * @throws ChangeRequestException in case of problem for loading information.
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

    /**
     * Check if the given file change is outdated.
     *
     * @param fileChange the file change to check if it is outdated.
     * @return {@code true} if the published version of the filechange does not match the current version of the
     *          document.
     * @throws ChangeRequestException in case of problems for loading the information.
     * @since 0.9
     */
    public boolean isFileChangeOutdated(FileChange fileChange) throws ChangeRequestException
    {
        return this.changeRequestManager.isFileChangeOutdated(fileChange);
    }

    /**
     * Check if the given template is supported by change request: if not supported, and it won't allow to
     * "save as change request" when using it.
     *
     * @param templateProviderReference the reference of the template.
     * @return {@code true} if it's supported.
     * @throws ChangeRequestException in case of problem when loading information.
     * @since 0.9
     */
    public boolean isTemplateSupported(DocumentReference templateProviderReference)
        throws ChangeRequestException
    {
        return this.changeRequestManager.isTemplateSupported(templateProviderReference);
    }

    /**
     * Define the minimum numbers of explicit users approvers needed: this minimum only concerns the explicit approvers
     * and it also only concerns the users approvers: groups are not counted, as well as group members.
     * Here 0 means that no minimum is required.
     *
     * @return the minimum needed of explicit approvers.
     * @since 0.13
     */
    public int getMinimumApprovers()
    {
        return this.configuration.getMinimumApprovers();
    }

    /**
     * Check that the minimum approvers configuration is respected.
     *
     * @param documentReference the document for which to check if the minimum number of approvers is respected.
     * @return {@code true} if no minimum is set, or if the minimum is respected for the given document.
     * @throws ChangeRequestException in case of problem to retrieve the approvers in the document.
     * @since 0.16
     */
    public boolean isMinimumApproversConfigurationRespected(DocumentReference documentReference)
        throws ChangeRequestException
    {
        boolean result = true;
        int minimumApprovers = getMinimumApprovers();
        if (minimumApprovers > 0) {
            int docApprovers = this.documentReferenceApproversManager.getAllApprovers(documentReference, false).size();
            result = minimumApprovers <= docApprovers;
        }
        return result;
    }

    /**
     * Get the html diff for the given file change.
     * Note that this throws an exception if the configuration doesn't enable it.
     *
     * @param fileChange the file change for which to get an html diff.
     * @return the html diff ready to be displayed.
     * @throws ChangeRequestException in case of problem to compute the diff.
     * @see ChangeRequestDiffManager#getHtmlDiff(FileChange)
     * @since 1.3
     */
    @Unstable
    public String getHtmlDiff(FileChange fileChange) throws ChangeRequestException
    {
        if (this.configuration.isRenderedDiffEnabled()) {
            return this.diffManager.getHtmlDiff(fileChange);
        } else {
            throw new ChangeRequestException("The rendered diff view is not enabled.");
        }
    }

    /**
     * @return {@code true} if the rendered diff feature is enabled.
     * @since 1.3
     */
    @Unstable
    public boolean isRenderedDiffEnabled()
    {
        return this.configuration.isRenderedDiffEnabled();
    }
}
