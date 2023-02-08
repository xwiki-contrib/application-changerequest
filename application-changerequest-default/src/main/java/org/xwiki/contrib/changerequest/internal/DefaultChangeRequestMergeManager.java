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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestManager;
import org.xwiki.contrib.changerequest.ChangeRequestMergeDocumentResult;
import org.xwiki.contrib.changerequest.ChangeRequestMergeManager;
import org.xwiki.contrib.changerequest.ConflictResolutionChoice;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.events.ChangeRequestConflictsFixedEvent;
import org.xwiki.contrib.changerequest.events.ChangeRequestUpdatedFileChangeEvent;
import org.xwiki.contrib.changerequest.events.ChangeRequestUpdatingFileChangeEvent;
import org.xwiki.contrib.changerequest.internal.cache.MergeCacheManager;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.contrib.changerequest.storage.FileChangeStorageManager;
import org.xwiki.diff.Conflict;
import org.xwiki.diff.ConflictDecision;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.store.merge.MergeConflictDecisionsManager;
import org.xwiki.store.merge.MergeDocumentResult;
import org.xwiki.store.merge.MergeManager;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.merge.MergeConfiguration;

/**
 * Default implementation of the {@link ChangeRequestMergeManager}.
 *
 * @version $Id$
 * @since 0.11
 */
@Component
@Singleton
public class DefaultChangeRequestMergeManager implements ChangeRequestMergeManager
{
    private static final String PREVIOUS_DOC_NOT_FOUND_LOGGER_MSG = "Cannot access the real previous version of "
        + "document for file change [{}]. "
        + "Using the current version as fallback.";

    @Inject
    private MergeManager mergeManager;

    @Inject
    private MergeConflictDecisionsManager mergeConflictDecisionsManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private FileChangeStorageManager fileChangeStorageManager;

    @Inject
    private ChangeRequestStorageManager changeRequestStorageManager;

    @Inject
    private FileChangeVersionManager fileChangeVersionManager;

    @Inject
    private UserReferenceResolver<CurrentUserReference> userReferenceResolver;

    @Inject
    private ObservationManager observationManager;

    @Inject
    private Provider<ChangeRequestManager> changeRequestManagerProvider;

    @Inject
    private MergeCacheManager mergeCacheManager;

    @Inject
    private Logger logger;

    @Override
    public boolean hasConflict(FileChange fileChange) throws ChangeRequestException
    {
        boolean result;
        Optional<Boolean> optional = this.mergeCacheManager.hasConflict(fileChange);
        if (optional.isPresent()) {
            result = optional.get();
        } else {
            switch (fileChange.getType()) {
                case DELETION:
                    result = deletionHasConflict(fileChange);
                    break;

                case CREATION:
                    result = creationHasConflict(fileChange);
                    break;

                default:
                case EDITION:
                    result = editionHasConflict(fileChange);
            }
            this.mergeCacheManager.setConflictStatus(fileChange, result);
        }
        return result;
    }

    private boolean creationHasConflict(FileChange fileChange) throws ChangeRequestException
    {
        DocumentModelBridge currentDoc =
            this.fileChangeStorageManager.getCurrentDocumentFromFileChange(fileChange);
        XWikiDocument xwikiCurrentDoc = (XWikiDocument) currentDoc;
        return !xwikiCurrentDoc.isNew();
    }

    private boolean deletionHasConflict(FileChange fileChange) throws ChangeRequestException
    {
        // You cannot create any conflict with a deletion:
        //   - case 1: the page still exists with original version and the CR request for deletion on same version:
        //             ideal situation, no conflict
        //   - case 2: the page still exists but has been updated, the CR request for deletion with a previous version:
        //             the CR can be refreshed to update the diff, but no conflict
        //   - case 3: the page has been already deleted, the CR request for deletion with a previous version:
        //             the CR can be refreshed, and when doing so the CR shows no change for this file
        return false;
    }

    private boolean editionHasConflict(FileChange fileChange) throws ChangeRequestException
    {
        DocumentModelBridge modifiedDoc =
            this.fileChangeStorageManager.getModifiedDocumentFromFileChange(fileChange);
        DocumentModelBridge originalDoc =
            this.fileChangeStorageManager.getCurrentDocumentFromFileChange(fileChange);

        DocumentModelBridge previousDoc;
        Optional<DocumentModelBridge> optionalPreviousDoc =
            this.fileChangeStorageManager.getPreviousDocumentFromFileChange(fileChange);
        if (optionalPreviousDoc.isEmpty()) {
            this.logger.debug(PREVIOUS_DOC_NOT_FOUND_LOGGER_MSG, fileChange);
            previousDoc = originalDoc;
        } else {
            previousDoc = optionalPreviousDoc.get();
        }

        XWikiContext context = this.contextProvider.get();
        MergeConfiguration mergeConfiguration = new MergeConfiguration();

        // We need the reference of the user and the document in the config to retrieve
        // the conflict decision in the MergeManager.
        mergeConfiguration.setUserReference(context.getUserReference());
        mergeConfiguration.setConcernedDocument(modifiedDoc.getDocumentReference());
        mergeConfiguration.setProvidedVersionsModifiables(false);

        MergeDocumentResult mergeDocumentResult =
            mergeManager.mergeDocument(previousDoc, originalDoc, modifiedDoc, mergeConfiguration);
        return mergeDocumentResult.hasConflicts();
    }

    @Override
    public boolean hasConflict(ChangeRequest changeRequest) throws ChangeRequestException
    {
        boolean result = false;
        Set<DocumentReference> documentReferences = changeRequest.getFileChanges().keySet();
        for (DocumentReference documentReference : documentReferences) {
            Optional<FileChange> fileChangeOptional =
                changeRequest.getLatestFileChangeFor(documentReference);
            if (fileChangeOptional.isPresent()) {
                FileChange fileChange = fileChangeOptional.get();
                if (this.hasConflict(fileChange)) {
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
        Optional<ChangeRequestMergeDocumentResult> optionalResult =
            this.mergeCacheManager.getChangeRequestMergeDocumentResult(fileChange);
        ChangeRequestMergeDocumentResult result;
        if (optionalResult.isPresent()) {
            result = optionalResult.get();
        } else {
            DocumentModelBridge currentDoc =
                this.fileChangeStorageManager.getCurrentDocumentFromFileChange(fileChange);
            XWikiDocument xwikiCurrentDoc = (XWikiDocument) currentDoc;
            XWikiDocument previousDoc;
            MergeDocumentResult mergeDocumentResult;
            Optional<DocumentModelBridge> optionalPreviousDoc;
            switch (fileChange.getType()) {
                case NO_CHANGE:
                    optionalPreviousDoc =
                        this.fileChangeStorageManager.getPreviousDocumentFromFileChange(fileChange);
                    previousDoc = (XWikiDocument) optionalPreviousDoc.orElse(null);
                    mergeDocumentResult =
                        new MergeDocumentResult(currentDoc, previousDoc, fileChange.getModifiedDocument());
                    result = new ChangeRequestMergeDocumentResult(mergeDocumentResult, false, fileChange,
                        currentDoc.getVersion(), currentDoc.getDate())
                        .setDocumentTitle(getTitle((XWikiDocument) currentDoc));
                    break;

                case DELETION:
                    optionalPreviousDoc =
                        this.fileChangeStorageManager.getPreviousDocumentFromFileChange(fileChange);
                    if (optionalPreviousDoc.isEmpty()) {
                        this.logger.debug(PREVIOUS_DOC_NOT_FOUND_LOGGER_MSG, fileChange);
                        previousDoc = xwikiCurrentDoc;
                    } else {
                        previousDoc = (XWikiDocument) optionalPreviousDoc.get();
                    }
                    mergeDocumentResult = new MergeDocumentResult(currentDoc, previousDoc, null);
                    boolean deletionConflict = this.deletionHasConflict(fileChange);
                    result = new ChangeRequestMergeDocumentResult(mergeDocumentResult, deletionConflict, fileChange,
                        previousDoc.getVersion(), previousDoc.getDate())
                        .setDocumentTitle(getTitle(xwikiCurrentDoc));
                    break;

                case CREATION:
                    boolean creationConflict = this.creationHasConflict(fileChange);
                    mergeDocumentResult =
                        new MergeDocumentResult(currentDoc, null, fileChange.getModifiedDocument());
                    result = new ChangeRequestMergeDocumentResult(mergeDocumentResult, creationConflict, fileChange,
                        currentDoc.getVersion(), currentDoc.getDate())
                        .setDocumentTitle(getTitle((XWikiDocument) fileChange.getModifiedDocument()));
                    break;

                case EDITION:
                    result = this.getEditionMergeDocumentResult(fileChange, xwikiCurrentDoc);
                    break;

                default:
                    throw new ChangeRequestException(String.format("Unknown file change type: [%s]", fileChange));
            }
            this.mergeCacheManager.setChangeRequestMergeDocumentResult(fileChange, result);
        }
        return result;
    }

    private ChangeRequestMergeDocumentResult getEditionMergeDocumentResult(FileChange fileChange,
        XWikiDocument xwikiCurrentDoc)
        throws ChangeRequestException
    {
        Optional<DocumentModelBridge> optionalPreviousDoc =
            this.fileChangeStorageManager.getPreviousDocumentFromFileChange(fileChange);
        XWikiDocument previousDoc;
        if (optionalPreviousDoc.isEmpty()) {
            this.logger.debug(PREVIOUS_DOC_NOT_FOUND_LOGGER_MSG, fileChange);
            previousDoc = xwikiCurrentDoc;
        } else {
            previousDoc = (XWikiDocument) optionalPreviousDoc.get();
        }
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
            mergeManager.mergeDocument(previousDoc, nextDoc, xwikiCurrentDoc, mergeConfiguration);
        return new ChangeRequestMergeDocumentResult(mergeDocumentResult, fileChange, previousDoc.getVersion(),
            previousDoc.getDate())
            .setDocumentTitle(getTitle((XWikiDocument) nextDoc));
    }

    private String getTitle(XWikiDocument document)
    {
        XWikiContext context = this.contextProvider.get();
        return document.getRenderedTitle(context);
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
        boolean result;
        // bulletproof to avoid NPE.
        List<ConflictDecision<?>> filteredDecisionList;
        ChangeRequest changeRequest = fileChange.getChangeRequest();
        this.observationManager.notify(new ChangeRequestUpdatingFileChangeEvent(), changeRequest.getId(),
            changeRequest);

        if (conflictDecisionList == null) {
            filteredDecisionList = Collections.emptyList();
        } else {
            filteredDecisionList = conflictDecisionList.stream().filter(Objects::nonNull).collect(Collectors.toList());
        }
        switch (fileChange.getType()) {
            case EDITION:
                result = this.handleEditionConflictDecision(fileChange, resolutionChoice, filteredDecisionList);
                break;

            case CREATION:
                if (!filteredDecisionList.isEmpty()) {
                    throw new ChangeRequestException("No support of custom decisions in case of fixing a conflict for "
                        + "a creation change.");
                }
                result = this.handleCreationConflictDecision(fileChange, resolutionChoice);
                break;

            case DELETION:
            case NO_CHANGE:
            default:
                throw new ChangeRequestException(
                    String.format("The following file change type does not support conflict fixing: [%s].",
                        fileChange.getType()));
        }

        if (result) {
            this.observationManager.notify(new ChangeRequestConflictsFixedEvent(), changeRequest.getId(), fileChange);
        }
        this.observationManager.notify(new ChangeRequestUpdatedFileChangeEvent(), changeRequest.getId(),
            changeRequest);

        return result;
    }

    private boolean handleCreationConflictDecision(FileChange fileChange, ConflictResolutionChoice resolutionChoice)
        throws ChangeRequestException
    {
        XWikiDocument currentDoc = (XWikiDocument)
            this.fileChangeStorageManager.getCurrentDocumentFromFileChange(fileChange);
        FileChange cloneFileChange;
        switch (resolutionChoice) {
            case CHANGE_REQUEST_VERSION:
                cloneFileChange = fileChange.cloneWithType(FileChange.FileChangeType.EDITION);
                break;

            case PUBLISHED_VERSION:
                cloneFileChange = fileChange.cloneWithType(FileChange.FileChangeType.NO_CHANGE);
                cloneFileChange.setModifiedDocument(currentDoc.clone());
                break;

            case CUSTOM:
            default:
                throw new ChangeRequestException(String.format("The following conflict resolution choice is not "
                    + "supported for creation request: [%s]", resolutionChoice));
        }
        cloneFileChange.setVersion(
            this.fileChangeVersionManager.getNextFileChangeVersion(fileChange.getVersion(), true));
        cloneFileChange.setPreviousPublishedVersion(currentDoc.getVersion(), currentDoc.getDate());
        cloneFileChange.setPreviousVersion(fileChange.getVersion());
        this.fileChangeStorageManager.save(cloneFileChange);

        return true;
    }

    private boolean handleEditionConflictDecision(FileChange fileChange, ConflictResolutionChoice resolutionChoice,
        List<ConflictDecision<?>> conflictDecisionList) throws ChangeRequestException
    {
        boolean result;
        DocumentReference targetEntity = fileChange.getTargetEntity();
        DocumentReference userReference = this.contextProvider.get().getUserReference();

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

        // We need to invalidate the cache so that the merge operation can occur again with the decisions.
        this.mergeCacheManager.invalidate(fileChange);
        // This second call is needed to actually perform the merge operation.
        mergeDocumentResult = this.getMergeDocumentResult(fileChange).getWrappedResult();
        if (mergeDocumentResult.hasConflicts()) {
            result = false;
        } else {
            String previousVersion = fileChange.getVersion();
            String previousPublishedVersion = mergeDocumentResult.getCurrentDocument().getVersion();
            Date previousPublishedVersionDate = mergeDocumentResult.getCurrentDocument().getDate();
            String version = this.fileChangeVersionManager.getNextFileChangeVersion(previousVersion, false);

            ChangeRequest changeRequest = fileChange.getChangeRequest();
            FileChange mergeFileChange = new FileChange(changeRequest)
                .setAuthor(this.userReferenceResolver.resolve(CurrentUserReference.INSTANCE))
                .setCreationDate(new Date())
                .setPreviousVersion(previousVersion)
                .setPreviousPublishedVersion(previousPublishedVersion, previousPublishedVersionDate)
                .setVersion(version)
                .setModifiedDocument(mergeDocumentResult.getMergeResult())
                .setTargetEntity(targetEntity);

            changeRequest.addFileChange(mergeFileChange);
            this.changeRequestStorageManager.save(changeRequest);
            this.changeRequestManagerProvider.get().computeReadyForMergingStatus(changeRequest);
            result = true;
        }
        return result;
    }
}
