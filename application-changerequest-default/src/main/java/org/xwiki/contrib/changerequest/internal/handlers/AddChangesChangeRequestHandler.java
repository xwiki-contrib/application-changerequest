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
package org.xwiki.contrib.changerequest.internal.handlers;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

import org.apache.http.HttpStatus;
import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestMergeManager;
import org.xwiki.contrib.changerequest.ChangeRequestReference;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.FileChangeSavingChecker;
import org.xwiki.contrib.changerequest.events.ChangeRequestFileChangeAddedEvent;
import org.xwiki.contrib.changerequest.events.ChangeRequestUpdatedFileChangeEvent;
import org.xwiki.contrib.changerequest.events.ChangeRequestUpdatingFileChangeEvent;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.store.merge.MergeDocumentResult;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiDocumentArchive;
import com.xpn.xwiki.web.EditForm;

/**
 * Handler for adding changes to an existing change request.
 *
 * @version $Id$
 * @since 0.3
 */
@Component
@Named("addchanges")
@Singleton
public class AddChangesChangeRequestHandler extends AbstractChangeRequestActionHandler
{
    @Inject
    private UserReferenceResolver<CurrentUserReference> userReferenceResolver;

    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Inject
    private ContextualLocalizationManager contextualLocalizationManager;

    @Inject
    private ChangeRequestMergeManager changeRequestMergeManager;

    @Override
    public void handle(ChangeRequestReference changeRequestReference) throws ChangeRequestException, IOException
    {
        HttpServletRequest request = this.prepareRequest();
        EditForm editForm = this.prepareForm(request);
        ChangeRequest changeRequest = this.loadChangeRequest(changeRequestReference);
        XWikiDocument modifiedDocument = this.prepareDocument(request, editForm, changeRequest);
        DocumentReference documentReference = modifiedDocument.getDocumentReferenceWithLocale();

        boolean isDeletion = "1".equals(request.getParameter("deletion"));

        FileChange.FileChangeType fileChangeType;
        if (isDeletion) {
            fileChangeType = FileChange.FileChangeType.DELETION;
        } else if (modifiedDocument.isNew()) {
            fileChangeType = FileChange.FileChangeType.CREATION;
        } else {
            fileChangeType = FileChange.FileChangeType.EDITION;
        }
        if (changeRequest != null) {
            UserReference currentUser = this.userReferenceResolver.resolve(CurrentUserReference.INSTANCE);
            FileChange fileChange =
                this.createFileChange(fileChangeType, changeRequest, modifiedDocument, documentReference, request,
                    currentUser);
            if (fileChange != null && this.checkDocumentCompatibility(changeRequest, fileChange)) {
                this.observationManager.notify(new ChangeRequestUpdatingFileChangeEvent(), changeRequest.getId(),
                    changeRequest);
                changeRequest.addFileChange(fileChange);
                this.storageManager.save(changeRequest);
                this.changeRequestRightsManager.copyViewRights(changeRequest, fileChange.getTargetEntity());
                this.copyApprovers(fileChange);
                this.observationManager
                    .notify(new ChangeRequestFileChangeAddedEvent(), changeRequest.getId(), fileChange);
                this.observationManager.notify(new ChangeRequestUpdatedFileChangeEvent(), changeRequest.getId(),
                    fileChange);
                this.responseSuccess(changeRequest);
            }
        }
    }

    private boolean checkDocumentCompatibility(ChangeRequest changeRequest, FileChange fileChange)
        throws ChangeRequestException, IOException
    {
        FileChangeSavingChecker.SavingCheckerResult result = new FileChangeSavingChecker.SavingCheckerResult();
        try {
            List<FileChangeSavingChecker> checkers =
                this.componentManager.getInstanceList(FileChangeSavingChecker.class);
            for (FileChangeSavingChecker checker : checkers) {
                result = checker.canChangeOnDocumentBeAdded(changeRequest, fileChange);
                if (!result.canBeSaved()) {
                    break;
                }
            }
            if (!result.canBeSaved()) {
                this.reportError(HttpStatus.SC_PRECONDITION_FAILED, result.getReason());
            }
        } catch (ComponentLookupException e) {
            throw new ChangeRequestException(String.format("Error when trying to load the compatibility checkers for "
                + "adding new changes from [%s] in [%s].", fileChange, changeRequest.getId()), e);
        }
        return result.canBeSaved();
    }

    private FileChange createFileChange(FileChange.FileChangeType fileChangeType, ChangeRequest changeRequest,
        XWikiDocument modifiedDocument, DocumentReference documentReference, HttpServletRequest request,
        UserReference currentUser)
        throws ChangeRequestException, IOException
    {
        FileChange fileChange = null;

        switch (fileChangeType) {
            case CREATION:
                fileChange = createFileChangeForCreation(changeRequest, modifiedDocument, documentReference, request);
                break;

            case DELETION:
                fileChange = createFileChangeForDeletion(changeRequest, modifiedDocument);
                break;

            case EDITION:
                fileChange = createFileChangeForEdition(changeRequest, modifiedDocument, documentReference, request);
                break;

            default:
                throw new ChangeRequestException(
                    String.format("Unknown file change type: [%s]", fileChange));
        }
        if (fileChange != null) {
            fileChange
                .setAuthor(currentUser)
                .setTargetEntity(documentReference);
        }

        return fileChange;
    }

    private FileChange createFileChangeForEdition(ChangeRequest changeRequest, XWikiDocument modifiedDocument,
        DocumentReference documentReference, HttpServletRequest request) throws ChangeRequestException, IOException
    {
        String previousVersion;
        FileChange fileChange;
        String fileChangeVersion;
        Optional<FileChange> previousFileChange;
        fileChange = new FileChange(changeRequest);

        previousFileChange = changeRequest.getLatestFileChangeFor(documentReference);
        previousVersion = request.getParameter(PREVIOUS_VERSION_PARAMETER);
        XWikiContext context = this.contextProvider.get();
        XWikiDocument previousDoc;
        try {
            XWikiDocumentArchive xWikiDocumentArchive =
                context.getWiki().getVersioningStore().getXWikiDocumentArchive(modifiedDocument, context);
            previousDoc = xWikiDocumentArchive.loadDocument(new Version(previousVersion), context);
        } catch (XWikiException e) {
            throw new ChangeRequestException(
                String.format("Error when trying to load previous version [%s] of doc [%s]", previousVersion,
                    modifiedDocument), e);
        }

        if (previousFileChange.isPresent()) {
            if (!this.addChangeToExistingFileChange(request, changeRequest, fileChange,
                previousFileChange.get(), modifiedDocument))
            {
                fileChange = null;
            }
        } else {
            if (!this.changeRequestRightsManager
                .isViewAccessConsistent(changeRequest, modifiedDocument.getDocumentReferenceWithLocale())) {

                // We're using 412 to distinguish with 409 data conflict, the right consistency can be seen
                // as a needed precondition for the request to be handled.
                this.reportError(HttpStatus.SC_PRECONDITION_FAILED, "changerequest.save.error.rightsconflict");
                fileChange = null;
            } else {
                fileChangeVersion =
                    this.fileChangeVersionManager.getNextFileChangeVersion(previousVersion, false);
                fileChange
                    .setVersion(fileChangeVersion)
                    .setPreviousVersion(previousVersion)
                    .setPreviousPublishedVersion(previousVersion, previousDoc.getDate())
                    .setModifiedDocument(modifiedDocument);
            }
        }
        return fileChange;
    }

    private FileChange createFileChangeForDeletion(ChangeRequest changeRequest, XWikiDocument modifiedDocument)
    {
        String previousVersion;
        String fileChangeVersion;
        FileChange fileChange;
        fileChange = new FileChange(changeRequest, FileChange.FileChangeType.DELETION);
        previousVersion = modifiedDocument.getVersion();
        fileChangeVersion =
            this.fileChangeVersionManager.getNextFileChangeVersion(previousVersion, false);
        fileChange
            .setVersion(fileChangeVersion)
            .setPreviousVersion(previousVersion)
            .setPreviousPublishedVersion(previousVersion, modifiedDocument.getDate());
        return fileChange;
    }

    private FileChange createFileChangeForCreation(ChangeRequest changeRequest, XWikiDocument modifiedDocument,
        DocumentReference documentReference, HttpServletRequest request) throws ChangeRequestException, IOException
    {
        Optional<FileChange> previousFileChange;
        FileChange fileChange;
        String fileChangeVersion;
        fileChange = new FileChange(changeRequest, FileChange.FileChangeType.CREATION);
        previousFileChange = changeRequest.getLatestFileChangeFor(documentReference);
        fileChangeVersion =
            this.fileChangeVersionManager.getNextFileChangeVersion("", false);
        fileChange
            .setVersion(fileChangeVersion)
            .setModifiedDocument(modifiedDocument);
        if (previousFileChange.isPresent()) {
            if (!this.addChangeToExistingFileChange(request, changeRequest, fileChange,
                previousFileChange.get(), modifiedDocument))
            {
                fileChange = null;
            }
        } else {
            fileChange
                .setVersion(fileChangeVersion)
                .setModifiedDocument(modifiedDocument);
        }
        return fileChange;
    }

    private boolean addChangeToExistingFileChange(HttpServletRequest request, ChangeRequest changeRequest,
        FileChange currentFileChange, FileChange latestFileChange, XWikiDocument modifiedDocument)
        throws ChangeRequestException, IOException
    {
        boolean result = false;
        String previousVersion = getPreviousVersion(request);
        String previousPublishedVersion = latestFileChange.getPreviousPublishedVersion();
        Date previousPublishedVersionDate = latestFileChange.getPreviousPublishedVersionDate();
        Optional<MergeDocumentResult> optionalMergeDocumentResult =
            this.changeRequestMergeManager.mergeDocumentChanges(modifiedDocument, previousVersion, changeRequest);
        if (optionalMergeDocumentResult.isPresent()) {
            MergeDocumentResult mergeDocumentResult = optionalMergeDocumentResult.get();
            String fileChangeVersion = this.fileChangeVersionManager.getNextFileChangeVersion(previousVersion, true);
            if (!mergeDocumentResult.hasConflicts()) {
                currentFileChange
                    .setPreviousPublishedVersion(previousPublishedVersion, previousPublishedVersionDate)
                    .setPreviousVersion(previousVersion)
                    .setVersion(fileChangeVersion)
                    .setModifiedDocument(mergeDocumentResult.getMergeResult());
                result = true;
            } else {
                this.reportError(HttpStatus.SC_CONFLICT, "changerequest.save.error.conflict");
            }
        } else {
            this.reportError(HttpStatus.SC_NOT_FOUND, "changerequest.save.error.notfound",
                modifiedDocument.getDocumentReferenceWithLocale().toString());
        }
        return result;
    }
}
