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
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestMergeManager;
import org.xwiki.contrib.changerequest.ChangeRequestReference;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.FileChangeSavingChecker;
import org.xwiki.contrib.changerequest.events.ChangeRequestFileChangeAddedEvent;
import org.xwiki.contrib.changerequest.events.ChangeRequestUpdatedFileChangeEvent;
import org.xwiki.contrib.changerequest.events.ChangeRequestUpdatingFileChangeEvent;
import org.xwiki.contrib.changerequest.internal.checkers.FileChangeSavingCheckersLoader;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.localization.LocalizationException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.store.merge.MergeDocumentResult;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiDocumentArchive;
import com.xpn.xwiki.web.EditForm;
import com.xpn.xwiki.web.Utils;

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
    private FileChangeSavingCheckersLoader fileChangeSavingCheckersLoader;

    @Inject
    private ChangeRequestMergeManager changeRequestMergeManager;

    @Inject
    private ContextualLocalizationManager contextualLocalizationManager;

    @Inject
    private Logger logger;

    @Override
    public void handle(ChangeRequestReference changeRequestReference)
        throws ChangeRequestException, IOException, XWikiException
    {
        HttpServletRequest request = this.prepareRequest();
        EditForm editForm = this.prepareForm(request);
        ChangeRequest changeRequest = this.loadChangeRequest(changeRequestReference);
        XWikiDocument modifiedDocument = this.prepareDocument(request, editForm, changeRequest);
        DocumentReference documentReference = modifiedDocument.getDocumentReferenceWithLocale();
        XWikiContext context = this.contextProvider.get();

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
                changeRequest
                    .addFileChange(fileChange)
                    .updateDate();
                Boolean isAjaxRequest = Utils.isAjaxRequest(context);
                try {
                    String saveComment =
                        this.contextualLocalizationManager.getTranslationPlain("changerequest.save.newchange");
                    this.storageManager.save(changeRequest, saveComment);
                } catch (Exception e) {
                    handleSaveException(isAjaxRequest, e, context);
                }

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
        List<FileChangeSavingChecker> checkers = this.fileChangeSavingCheckersLoader.getCheckers();
        for (FileChangeSavingChecker checker : checkers) {
            result = checker.canChangeOnDocumentBeAdded(changeRequest, fileChange);
            if (!result.canBeSaved()) {
                break;
            }
        }
        if (!result.canBeSaved()) {
            this.reportError(HttpStatus.SC_PRECONDITION_FAILED, result.getReason());
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
        boolean isMinorEdit = "1".equals(request.getParameter("isMinorChange"));
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
                previousFileChange.get(), modifiedDocument, isMinorEdit))
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
                    .setModifiedDocument(modifiedDocument)
                    .setMinorChange(isMinorEdit);
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
                previousFileChange.get(), modifiedDocument, false))
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
        FileChange currentFileChange, FileChange latestFileChange, XWikiDocument modifiedDocument, boolean isMinorEdit)
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
                    .setModifiedDocument(mergeDocumentResult.getMergeResult())
                    .setMinorChange(isMinorEdit);
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

    /**
     * @param isAjaxRequest Indicate if this is an ajax request.
     * @param exception The exception to handle.
     * @param context The XWiki context.
     * @throws XWikiException unless it is an ajax request.
     */
    private void handleSaveException(boolean isAjaxRequest, Exception exception, XWikiContext context)
        throws XWikiException
    {
        if (isAjaxRequest) {
            String errorMessage =
                localizePlainOrReturnKey("core.editors.saveandcontinue.exceptionWhileSaving", exception.getMessage());

            writeAjaxErrorResponse(javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR, errorMessage,
                context);

            String logMessage = "Caught exception during save and continue";
            if (exception instanceof XWikiException) {
                logger.info(logMessage, exception);
            } else {
                logger.error(logMessage, exception);
            }
        } else {
            if (exception instanceof XWikiException) {
                throw (XWikiException) exception;
            } else {
                throw new XWikiException(XWikiException.MODULE_XWIKI_APP, XWikiException.ERROR_XWIKI_UNKNOWN,
                    "Uncaught exception", exception);
            }
        }
    }

    /**
     * Write an error response to an ajax request.
     *
     * @param httpStatusCode The status code to set on the response.
     * @param message The message that should be displayed.
     * @param context the context.
     */
    private void writeAjaxErrorResponse(int httpStatusCode, String message, XWikiContext context)
    {
        try {
            context.getResponse().setContentType("text/plain");
            context.getResponse().setStatus(httpStatusCode);
            context.getResponse().setCharacterEncoding(context.getWiki().getEncoding());
            context.getResponse().getWriter().print(message);
        } catch (IOException e) {
            logger.error("Failed to send error response to AJAX save and continue request.", e);
        }
    }

    private String localizePlainOrReturnKey(String key, Object... parameters)
    {
        return localizeOrReturnKey(key, Syntax.PLAIN_1_0, parameters);
    }

    protected String localizeOrReturnKey(String key, Syntax syntax, Object... parameters)
    {
        String result;
        try {
            result = Objects.toString(contextualLocalizationManager.getTranslation(key, syntax, parameters), key);
        } catch (LocalizationException e) {
            // Return the key in case of error but log a warning
            logger.warn("Error rendering the translation for key [{}] in syntax [{}]. Using the translation key "
                + "instead. Root cause: [{}]", key, syntax.toIdString(), ExceptionUtils.getRootCauseMessage(e));
            result = key;
        }
        return result;
    }
}
