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
package org.xwiki.contrib.changerequest.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestConfiguration;
import org.xwiki.contrib.changerequest.ChangeRequestManager;
import org.xwiki.contrib.changerequest.ChangeRequestMergeDocumentResult;
import org.xwiki.contrib.changerequest.ChangeRequestStatus;
import org.xwiki.contrib.changerequest.ConflictResolutionChoice;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.MergeApprovalStrategy;
import org.xwiki.contrib.changerequest.rights.ChangeRequestApproveRight;
import org.xwiki.contrib.changerequest.rights.ChangeRequestRight;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.contrib.changerequest.storage.FileChangeStorageManager;
import org.xwiki.diff.Conflict;
import org.xwiki.diff.ConflictDecision;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.store.merge.MergeConflictDecisionsManager;
import org.xwiki.store.merge.MergeDocumentResult;
import org.xwiki.store.merge.MergeManager;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.merge.MergeConfiguration;

/**
 * Default implementation of {@link ChangeRequestManager}.
 *
 * @version $Id$
 * @since 0.1
 */
@Component
@Singleton
public class DefaultChangeRequestManager implements ChangeRequestManager
{
    @Inject
    private FileChangeStorageManager fileChangeStorageManager;

    @Inject
    private MergeManager mergeManager;

    @Inject
    private MergeConflictDecisionsManager mergeConflictDecisionsManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    private UserReferenceConverter userReferenceConverter;

    @Inject
    private UserReferenceResolver<CurrentUserReference> userReferenceResolver;

    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Inject
    private ChangeRequestConfiguration configuration;

    @Inject
    private FileChangeVersionManager fileChangeVersionManager;

    @Inject
    private ChangeRequestStorageManager changeRequestStorageManager;

    @Override
    public boolean hasConflicts(FileChange fileChange) throws ChangeRequestException
    {
        switch (fileChange.getType()) {
            case EDITION:
                return editionHasConflict(fileChange);

            case DELETION:
                return deletionHasConflict(fileChange);

            case CREATION:
            default:
                throw new ChangeRequestException("Not yet implemented.");
        }
    }

    private boolean deletionHasConflict(FileChange fileChange) throws ChangeRequestException
    {
        DocumentModelBridge currentDoc =
            this.fileChangeStorageManager.getCurrentDocumentFromFileChange(fileChange);
        XWikiDocument xwikiCurrentDoc = (XWikiDocument) currentDoc;
        return xwikiCurrentDoc.isNew()
            || !(currentDoc.getVersion().equals(fileChange.getPreviousPublishedVersion()));
    }

    private boolean editionHasConflict(FileChange fileChange) throws ChangeRequestException
    {
        DocumentModelBridge modifiedDoc =
            this.fileChangeStorageManager.getModifiedDocumentFromFileChange(fileChange);
        DocumentModelBridge previousDoc =
            this.fileChangeStorageManager.getPreviousDocumentFromFileChange(fileChange);
        DocumentModelBridge originalDoc =
            this.fileChangeStorageManager.getCurrentDocumentFromFileChange(fileChange);

        XWikiContext context = this.contextProvider.get();
        MergeConfiguration mergeConfiguration = new MergeConfiguration();

        // We need the reference of the user and the document in the config to retrieve
        // the conflict decision in the MergeManager.
        mergeConfiguration.setUserReference(context.getUserReference());
        mergeConfiguration.setConcernedDocument(modifiedDoc.getDocumentReference());

        // The modified doc is actually the one we should save, so it's ok to modify it directly
        // and better for performance.
        mergeConfiguration.setProvidedVersionsModifiables(false);

        MergeDocumentResult mergeDocumentResult =
            mergeManager.mergeDocument(previousDoc, originalDoc, modifiedDoc, mergeConfiguration);
        return mergeDocumentResult.hasConflicts();
    }

    @Override
    public boolean isAuthorizedToMerge(UserReference userReference, ChangeRequest changeRequest)
    {
        boolean result = true;
        DocumentReference userDocReference = this.userReferenceConverter.convert(userReference);
        Right approvalRight = ChangeRequestApproveRight.getRight();
        for (DocumentReference documentReference : changeRequest.getModifiedDocuments()) {
            if (!this.authorizationManager.hasAccess(Right.EDIT, userDocReference, documentReference)
                || !this.authorizationManager.hasAccess(approvalRight, userDocReference, documentReference)) {
                result = false;
                break;
            }
        }

        return result;
    }

    @Override
    public MergeApprovalStrategy getMergeApprovalStrategy() throws ChangeRequestException
    {
        try {
            return this.componentManager.getInstance(MergeApprovalStrategy.class,
                this.configuration.getMergeApprovalStrategy());
        } catch (ComponentLookupException e) {
            throw new ChangeRequestException("Error when getting the merge approval strategy", e);
        }
    }

    @Override
    public boolean canBeMerged(ChangeRequest changeRequest) throws ChangeRequestException
    {
        boolean result = false;
        if (changeRequest.getStatus() == ChangeRequestStatus.READY_FOR_REVIEW) {
            MergeApprovalStrategy mergeApprovalStrategy = getMergeApprovalStrategy();
            if (mergeApprovalStrategy.canBeMerged(changeRequest)) {
                result = !this.hasConflict(changeRequest);
            }
        }
        return result;
    }

    private boolean hasConflict(ChangeRequest changeRequest) throws ChangeRequestException
    {
        boolean result = false;
        Set<DocumentReference> documentReferences = changeRequest.getFileChanges().keySet();
        for (DocumentReference documentReference : documentReferences) {
            Optional<FileChange> fileChangeOptional =
                changeRequest.getLatestFileChangeFor(documentReference);
            if (fileChangeOptional.isPresent()) {
                FileChange fileChange = fileChangeOptional.get();
                if (this.hasConflicts(fileChange)) {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public ChangeRequestMergeDocumentResult getMergeDocumentResult(FileChange fileChange)
        throws ChangeRequestException
    {
        DocumentModelBridge currentDoc =
            this.fileChangeStorageManager.getCurrentDocumentFromFileChange(fileChange);
        ChangeRequestMergeDocumentResult result;
        if (fileChange.getType() == FileChange.FileChangeType.DELETION) {
            XWikiDocument xwikiCurrentDoc = (XWikiDocument) currentDoc;
            boolean deletionConflict = xwikiCurrentDoc.isNew()
                || !(currentDoc.getVersion().equals(fileChange.getPreviousPublishedVersion()));
            result = new ChangeRequestMergeDocumentResult(deletionConflict).setDocumentTitle(getTitle(xwikiCurrentDoc));
        } else {
            DocumentModelBridge previousDoc =
                this.fileChangeStorageManager.getPreviousDocumentFromFileChange(fileChange);
            DocumentModelBridge nextDoc =
                this.fileChangeStorageManager.getModifiedDocumentFromFileChange(fileChange);

            MergeConfiguration mergeConfiguration = new MergeConfiguration();
            DocumentReference documentReference = fileChange.getTargetEntity();

            XWikiContext context = this.contextProvider.get();
            // We need the reference of the user and the document in the config to retrieve
            // the conflict decision in the MergeManager.
            mergeConfiguration.setUserReference(context.getUserReference());
            mergeConfiguration.setConcernedDocument(documentReference);

            mergeConfiguration.setProvidedVersionsModifiables(false);
            MergeDocumentResult mergeDocumentResult =
                mergeManager.mergeDocument(previousDoc, nextDoc, currentDoc, mergeConfiguration);
            result = new ChangeRequestMergeDocumentResult(mergeDocumentResult)
                .setDocumentTitle(getTitle((XWikiDocument) mergeDocumentResult.getCurrentDocument()));
        }
        return result;
    }

    private String getTitle(XWikiDocument document)
    {
        if (document.isNew()) {
            return document.getDocumentReference().toString();
        } else {
            XWikiContext context = this.contextProvider.get();
            return document.getRenderedTitle(context);
        }
    }

    @Override
    public Optional<MergeDocumentResult> mergeDocumentChanges(DocumentModelBridge modifiedDocument,
        String previousVersion, ChangeRequest changeRequest) throws ChangeRequestException
    {
        Map<DocumentReference, Deque<FileChange>> fileChangesMap = changeRequest.getFileChanges();
        XWikiDocument nextDoc = (XWikiDocument) modifiedDocument;
        DocumentReference documentReference = nextDoc.getDocumentReferenceWithLocale();
        if (fileChangesMap.containsKey(documentReference)) {
            Deque<FileChange> fileChanges = fileChangesMap.get(documentReference);
            boolean isPreviousFromFilechange = this.fileChangeVersionManager.isFileChangeVersion(previousVersion);
            FileChange lastFileChange = fileChanges.getLast();
            FileChange fileChange = null;
            DocumentModelBridge previousDoc;
            DocumentModelBridge currentDoc;
            if (isPreviousFromFilechange) {
                Iterator<FileChange> fileChangeIterator = fileChanges.descendingIterator();
                FileChange checkingFileChange;
                while (fileChangeIterator.hasNext()) {
                    checkingFileChange = fileChangeIterator.next();
                    if (previousVersion.equals(checkingFileChange.getVersion())) {
                        fileChange = checkingFileChange;
                    }
                }
                if (fileChange == null) {
                    throw new ChangeRequestException(
                        String.format("Cannot find file change with version [%s]", previousVersion));
                }
                previousDoc = this.fileChangeStorageManager.getModifiedDocumentFromFileChange(fileChange);
                currentDoc = this.fileChangeStorageManager.getModifiedDocumentFromFileChange(lastFileChange);
            } else {
                fileChange = lastFileChange;
                previousDoc = this.fileChangeStorageManager.getDocumentFromFileChange(fileChange, previousVersion);
                currentDoc = this.fileChangeStorageManager.getModifiedDocumentFromFileChange(fileChange);
            }

            XWikiContext context = this.contextProvider.get();
            MergeConfiguration mergeConfiguration = new MergeConfiguration();

            // We need the reference of the user and the document in the config to retrieve
            // the conflict decision in the MergeManager.
            mergeConfiguration.setUserReference(context.getUserReference());
            mergeConfiguration.setConcernedDocument(documentReference);

            // The modified doc is actually the one we should save, so it's ok to modify it directly
            // and better for performance.
            mergeConfiguration.setProvidedVersionsModifiables(true);
            MergeDocumentResult mergeDocumentResult =
                mergeManager.mergeDocument(previousDoc, currentDoc, modifiedDocument, mergeConfiguration);

            return Optional.of(mergeDocumentResult);
        }

        return Optional.empty();
    }

    @Override
    public boolean mergeWithConflictDecision(FileChange fileChange, ConflictResolutionChoice resolutionChoice,
        List<ConflictDecision<?>> conflictDecisionList) throws ChangeRequestException
    {
        DocumentReference targetEntity = fileChange.getTargetEntity();
        DocumentReference userReference = this.contextProvider.get().getUserReference();

        if (fileChange.getType() == FileChange.FileChangeType.EDITION) {
            MergeDocumentResult mergeDocumentResult = this.getMergeDocumentResult(fileChange).getWrappedResult();

            ArrayList<Conflict<?>> conflicts = new ArrayList<>(mergeDocumentResult.getConflicts());
            // FIXME: only handle content conflicts for now, see XWIKI-18908
            this.mergeConflictDecisionsManager.recordConflicts(fileChange.getTargetEntity(), userReference,
                conflicts);

            ConflictDecision.DecisionType globalDecisionType = null;

            switch (resolutionChoice) {
                case CUSTOM:
                    this.mergeConflictDecisionsManager
                        .setConflictDecisionList(new ArrayList<>(conflictDecisionList), targetEntity, userReference);
                    break;

                case CHANGE_REQUEST_VERSION:
                    globalDecisionType = ConflictDecision.DecisionType.NEXT;
                    break;

                case PUBLISHED_VERSION:
                    globalDecisionType = ConflictDecision.DecisionType.CURRENT;
                    break;

                default:
                    globalDecisionType = ConflictDecision.DecisionType.UNDECIDED;
                    break;
            }

            if (globalDecisionType != null) {
                for (Conflict<?> conflict : conflicts) {
                    this.mergeConflictDecisionsManager.recordDecision(targetEntity, userReference,
                        conflict.getReference(),
                        globalDecisionType, Collections.emptyList());
                }
            }

            mergeDocumentResult = this.getMergeDocumentResult(fileChange).getWrappedResult();
            if (mergeDocumentResult.hasConflicts()) {
                return false;
            } else {
                String previousVersion = fileChange.getVersion();
                String previousPublishedVersion = mergeDocumentResult.getCurrentDocument().getVersion();
                String version = this.fileChangeVersionManager.getNextFileChangeVersion(previousVersion, false);

                ChangeRequest changeRequest = fileChange.getChangeRequest();
                FileChange mergeFileChange = new FileChange(changeRequest)
                    .setAuthor(this.userReferenceResolver.resolve(CurrentUserReference.INSTANCE))
                    .setCreationDate(new Date())
                    .setPreviousVersion(previousVersion)
                    .setPreviousPublishedVersion(previousPublishedVersion)
                    .setVersion(version)
                    .setModifiedDocument(mergeDocumentResult.getMergeResult())
                    .setTargetEntity(targetEntity);

                changeRequest.addFileChange(mergeFileChange);
                this.changeRequestStorageManager.save(changeRequest);
                return true;
            }
        } else {
            // handle deletion conflict
            return false;
        }
    }

    @Override
    public boolean isAuthorizedToReview(UserReference userReference, ChangeRequest changeRequest)
    {
        boolean result = false;
        DocumentReference userDocReference = this.userReferenceConverter.convert(userReference);
        Right approvalRight = ChangeRequestApproveRight.getRight();
        if (!(changeRequest.getAuthors().contains(userReference))) {
            result = true;
            for (DocumentReference documentReference : changeRequest.getModifiedDocuments()) {
                if (!this.authorizationManager.hasAccess(approvalRight, userDocReference, documentReference)) {
                    result = false;
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public boolean isAuthorizedToFixConflict(UserReference userReference, FileChange fileChange)
    {
        DocumentReference userDocReference = this.userReferenceConverter.convert(userReference);
        Right changeRequestRight = ChangeRequestRight.getRight();
        return this.authorizationManager.hasAccess(changeRequestRight, userDocReference, fileChange.getTargetEntity());
    }
}
