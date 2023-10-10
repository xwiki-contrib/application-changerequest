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

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestConfiguration;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestManager;
import org.xwiki.contrib.changerequest.ChangeRequestMergeManager;
import org.xwiki.contrib.changerequest.ChangeRequestReview;
import org.xwiki.contrib.changerequest.ChangeRequestStatus;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.MergeApprovalStrategy;
import org.xwiki.contrib.changerequest.events.ChangeRequestRebasedEvent;
import org.xwiki.contrib.changerequest.events.ChangeRequestReviewAddedEvent;
import org.xwiki.contrib.changerequest.events.ChangeRequestStatusChangedEvent;
import org.xwiki.contrib.changerequest.events.FileChangeRebasedEvent;
import org.xwiki.contrib.changerequest.internal.cache.ChangeRequestTitleCacheManager;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.contrib.changerequest.storage.FileChangeStorageManager;
import org.xwiki.contrib.changerequest.storage.ReviewStorageManager;
import org.xwiki.extension.xar.script.XarExtensionScriptService;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.script.service.ScriptService;
import org.xwiki.user.UserReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Default implementation of {@link ChangeRequestManager}.
 *
 * @version $Id$
 * @since 0.1
 */
@Component
@Singleton
public class DefaultChangeRequestManager implements ChangeRequestManager, Initializable
{
    @Inject
    private FileChangeStorageManager fileChangeStorageManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Inject
    private ChangeRequestConfiguration configuration;

    @Inject
    private ChangeRequestStorageManager changeRequestStorageManager;

    @Inject
    private ReviewStorageManager reviewStorageManager;

    @Inject
    private ObservationManager observationManager;

    @Inject
    private TemplateProviderSupportChecker templateProviderSupportChecker;

    @Inject
    private ChangeRequestMergeManager changeRequestMergeManager;

    @Inject
    private ChangeRequestTitleCacheManager titleCacheManager;

    @Inject
    private ContextualLocalizationManager localizationManager;

    private XarExtensionScriptService xarExtensionScriptService;

    @Override
    public void initialize() throws InitializationException
    {
        try {
            this.xarExtensionScriptService = this.componentManager.getInstance(ScriptService.class, "extension.xar");
        } catch (ComponentLookupException e) {
            throw new InitializationException("Error while getting the XarExtensionScriptService", e);
        }
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

    private String getUpdateStatusSaveComment()
    {
        return this.localizationManager.getTranslationPlain("changerequest.save.changestatus");
    }

    @Override
    public void computeReadyForMergingStatus(ChangeRequest changeRequest) throws ChangeRequestException
    {
        ChangeRequestStatus status = changeRequest.getStatus();
        boolean readyForMerging = false;
        if (status == ChangeRequestStatus.READY_FOR_REVIEW || status == ChangeRequestStatus.READY_FOR_MERGING) {
            MergeApprovalStrategy mergeApprovalStrategy = getMergeApprovalStrategy();
            if (mergeApprovalStrategy.canBeMerged(changeRequest)) {
                readyForMerging = !this.changeRequestMergeManager.hasConflict(changeRequest);
            }
        }
        boolean update = false;
        ChangeRequestStatus newStatus = null;
        if (status == ChangeRequestStatus.READY_FOR_REVIEW && readyForMerging) {
            newStatus = ChangeRequestStatus.READY_FOR_MERGING;
            update = true;
        } else if (status == ChangeRequestStatus.READY_FOR_MERGING && !readyForMerging) {
            newStatus = ChangeRequestStatus.READY_FOR_REVIEW;
            update = true;
        }
        if (update) {
            changeRequest
                .setStatus(newStatus)
                .updateDate();
            this.changeRequestStorageManager.save(changeRequest, getUpdateStatusSaveComment());
            this.observationManager.notify(new ChangeRequestStatusChangedEvent(), changeRequest.getId(),
                new ChangeRequestStatus[] {status, newStatus});
        }
    }

    @Override
    public boolean canBeMerged(ChangeRequest changeRequest) throws ChangeRequestException
    {
        return changeRequest.getStatus() == ChangeRequestStatus.READY_FOR_MERGING;
    }

    @Override
    public boolean canDeletionBeRequested(DocumentReference documentReference) throws ChangeRequestException
    {
        boolean result = false;
        if (!this.xarExtensionScriptService.isExtensionDocument(documentReference)) {
            XWikiContext context = this.contextProvider.get();
            try {
                XWikiDocument document = context.getWiki().getDocument(documentReference, context);
                boolean hasClass = document.getXClass().getProperties().length > 0;
                boolean isHidden = document.isHidden();
                result = !hasClass && !isHidden;
            } catch (XWikiException e) {
                throw new ChangeRequestException(
                    String.format("Error when loading document [%s] for checking properties", e));
            }
        }
        return result;
    }

    @Override
    public void updateStatus(ChangeRequest changeRequest, ChangeRequestStatus newStatus)
        throws ChangeRequestException
    {
        ChangeRequestStatus oldStatus = changeRequest.getStatus();
        if (oldStatus != newStatus) {
            changeRequest.setStatus(newStatus);
            this.changeRequestStorageManager.save(changeRequest, getUpdateStatusSaveComment());
            this.observationManager.notify(new ChangeRequestStatusChangedEvent(), changeRequest.getId(),
                new ChangeRequestStatus[] {oldStatus, newStatus});
            this.computeReadyForMergingStatus(changeRequest);
        }
    }

    @Override
    public ChangeRequestReview addReview(ChangeRequest changeRequest, UserReference reviewer, boolean approved,
        UserReference originalApprover) throws ChangeRequestException
    {
        Optional<ChangeRequestReview> optionalLatestReview;
        ChangeRequestReview review = new ChangeRequestReview(changeRequest, approved, reviewer);
        review.setNew(true);
        if (originalApprover != null && !originalApprover.equals(reviewer)) {
            review.setOriginalApprover(originalApprover);
            optionalLatestReview =  changeRequest.getLatestReviewFromOrOnBehalfOf(originalApprover);
        } else {
            optionalLatestReview = changeRequest.getLatestReviewFromOrOnBehalfOf(reviewer);
        }
        this.reviewStorageManager.save(review);

        // ensure previous review from latest author is considered outdated
        if (optionalLatestReview.isPresent()) {
            ChangeRequestReview previousReview = optionalLatestReview.get();
            previousReview.setLastFromAuthor(false);
            // if the review is already invalidated, we don't need to do anything.
            if (previousReview.isValid()) {
                previousReview.setValid(false);
                previousReview.setSaved(false);
                this.reviewStorageManager.save(previousReview);
            }
        }
        changeRequest
            .addReview(review)
            .updateDate();
        // In theory this should never be needed with the default storage as it already update the CR document.
        this.changeRequestStorageManager.save(changeRequest, "changerequest.save.addReview");
        this.observationManager.notify(new ChangeRequestReviewAddedEvent(), changeRequest.getId(), review);
        this.computeReadyForMergingStatus(changeRequest);
        return review;
    }

    @Override
    public boolean isFileChangeOutdated(FileChange fileChange) throws ChangeRequestException
    {
        XWikiDocument currentDocument =
            (XWikiDocument) this.fileChangeStorageManager.getCurrentDocumentFromFileChange(fileChange);
        boolean result = false;

        switch (fileChange.getType()) {
            case CREATION:
                result = !currentDocument.isNew();
                break;

            case DELETION:
                result = currentDocument.isNew() || this.isVersionOutdated(fileChange, currentDocument);
                break;

            case NO_CHANGE:
                Optional<FileChange> fileChangeWithChange =
                    fileChange.getChangeRequest().getFileChangeWithChangeBefore(fileChange);
                if (fileChangeWithChange.isPresent()) {
                    result = this.isNoChangeFileChangeOutdated(fileChange, fileChangeWithChange.get(), currentDocument);
                } else {
                    throw new ChangeRequestException(
                        String.format("Cannot find a filechange with actual change before [%s]", fileChange));
                }
                break;

            case EDITION:
                result = this.isVersionOutdated(fileChange, currentDocument);
                break;

            default:
                throw new ChangeRequestException(String.format("Unsupported type: [%s]", fileChange.getType()));
        }
        return result;
    }

    private boolean isNoChangeFileChangeOutdated(FileChange originalFileChange, FileChange fileChangeWithChanges,
        XWikiDocument currentDocument)
        throws ChangeRequestException
    {
        boolean result = false;
        switch (fileChangeWithChanges.getType()) {
            case CREATION:
            case EDITION:
                result = this.isVersionOutdated(originalFileChange, currentDocument);
                break;

            case DELETION:
                result = !currentDocument.isNew();
                break;

            default:
                throw new ChangeRequestException(String.format("Unsupported type for outdated filechange: [%s]",
                    fileChangeWithChanges.getType()));
        }

        return result;
    }

    private boolean isVersionOutdated(FileChange fileChange, XWikiDocument currentDocument)
    {
        String previousPublishedVersion = fileChange.getPreviousPublishedVersion();
        String currentDocumentVersion = currentDocument.getVersion();
        boolean result;
        // if version are equals, we check date to ensure the version are still same
        if (StringUtils.equals(previousPublishedVersion, currentDocumentVersion)) {
            result = fileChange.getPreviousPublishedVersionDate().before(currentDocument.getDate());
            // if version are not equals, we don't care if it's because it's a new change has been added or a
            // version has been removed: either way the filechange is outdated.
        } else {
            result = true;
        }
        return result;
    }

    @Override
    public boolean isTemplateProviderSupported(DocumentReference templateProviderReference)
        throws ChangeRequestException
    {
        return this.templateProviderSupportChecker.isTemplateProviderSupported(templateProviderReference);
    }

    @Override
    public boolean isTemplateSupported(DocumentReference templateReference) throws ChangeRequestException
    {
        return this.templateProviderSupportChecker.isTemplateSupported(templateReference);
    }

    @Override
    public void rebase(ChangeRequest changeRequest) throws ChangeRequestException
    {
        for (FileChange fileChange : changeRequest.getLastFileChanges()) {
            this.fileChangeStorageManager.rebase(fileChange);
        }
        this.observationManager.notify(new ChangeRequestRebasedEvent(), changeRequest.getId(), changeRequest);
    }

    @Override
    public void rebase(FileChange fileChange) throws ChangeRequestException
    {
        this.fileChangeStorageManager.rebase(fileChange);
        this.observationManager.notify(new FileChangeRebasedEvent(), fileChange.getChangeRequest().getId(), fileChange);
    }

    @Override
    public void invalidateReviews(ChangeRequest changeRequest) throws ChangeRequestException
    {
        List<ChangeRequestReview> reviews = changeRequest.getReviews();
        for (ChangeRequestReview review : reviews) {
            if (review.isApproved() && review.isValid()) {
                review.setValid(false);
                review.setSaved(false);
                this.reviewStorageManager.save(review);
            }
        }
    }

    @Override
    public String getTitle(String changeRequestId, String fileChangeId)
    {
        return this.titleCacheManager.getTitle(changeRequestId, fileChangeId);
    }

    @Override
    public void eraseChangesFor(DocumentReference documentReference) throws ChangeRequestException
    {
        for (ChangeRequest changeRequest : this.changeRequestStorageManager.
            findChangeRequestTargeting(documentReference)) {
            if (changeRequest.getModifiedDocuments().size() == 1) {
                this.changeRequestStorageManager.delete(changeRequest);
            } else {
                this.changeRequestStorageManager.split(changeRequest, Set.of(documentReference));
            }
        }
    }
}
