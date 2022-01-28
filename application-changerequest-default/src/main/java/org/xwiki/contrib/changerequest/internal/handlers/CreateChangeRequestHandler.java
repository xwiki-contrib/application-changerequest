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
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ApproversManager;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestManager;
import org.xwiki.contrib.changerequest.ChangeRequestReference;
import org.xwiki.contrib.changerequest.ChangeRequestRightsManager;
import org.xwiki.contrib.changerequest.ChangeRequestStatus;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.events.ChangeRequestCreatedEvent;
import org.xwiki.contrib.changerequest.internal.FileChangeVersionManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiDocumentArchive;
import com.xpn.xwiki.web.EditForm;

/**
 * Specific handler for creating a new change request.
 *
 * @version $Id$
 * @since 0.1
 */
@Component
@Named("create")
@Singleton
public class CreateChangeRequestHandler extends AbstractChangeRequestActionHandler
{
    @Inject
    private UserReferenceResolver<CurrentUserReference> userReferenceResolver;

    @Inject
    private FileChangeVersionManager fileChangeVersionManager;

    @Inject
    private ApproversManager<DocumentReference> documentReferenceApproversManager;

    @Inject
    private ApproversManager<ChangeRequest> changeRequestApproversManager;

    @Inject
    private ChangeRequestManager changeRequestManager;

    @Inject
    private ChangeRequestRightsManager changeRequestRightsManager;

    /**
     * Handle the given {@link ChangeRequestReference} for performing the create.
     * @param changeRequestReference the request reference leading to this.
     * @throws ChangeRequestException in case of errors.
     */
    @Override
    public void handle(ChangeRequestReference changeRequestReference) throws ChangeRequestException, IOException
    {
        HttpServletRequest request = this.prepareRequest();
        ChangeRequest changeRequest = getChangeRequest(request);
        this.storageManager.save(changeRequest);
        this.changeRequestRightsManager.copyViewRights(changeRequest,
            changeRequest.getModifiedDocuments().iterator().next());
        this.copyApprovers(changeRequest);
        this.changeRequestManager.computeReadyForMergingStatus(changeRequest);
        this.observationManager.notify(new ChangeRequestCreatedEvent(), changeRequest.getId(), changeRequest);

        this.responseSuccess(changeRequest);
    }

    private XWikiDocument getModifiedDocument(HttpServletRequest request, boolean isDeletion, XWikiContext context)
        throws ChangeRequestException
    {
        XWikiDocument modifiedDocument;
        if (!isDeletion) {
            EditForm editForm = this.prepareForm(request);
            modifiedDocument = this.prepareDocument(request, editForm, null);
        } else {
            // TODO: Handle affectChildren
            String serializedReference = request.getParameter("docReference");
            DocumentReference referenceWithoutLocale = this.documentReferenceResolver.resolve(serializedReference);
            String localeString = request.getParameter("locale");
            Locale locale;
            if (StringUtils.isEmpty(localeString)) {
                locale = Locale.ROOT;
            } else {
                locale = LocaleUtils.toLocale(localeString);
            }
            DocumentReference documentReference = new DocumentReference(referenceWithoutLocale, locale);
            try {
                modifiedDocument = context.getWiki().getDocument(documentReference, context);
            } catch (XWikiException e) {
                throw new ChangeRequestException(
                    String.format("Cannot read document [%s]", documentReference), e);
            }
        }
        return modifiedDocument;
    }

    private ChangeRequest getChangeRequest(HttpServletRequest request) throws ChangeRequestException
    {
        boolean isDeletion = "1".equals(request.getParameter("deletion"));

        XWikiContext context = this.contextProvider.get();
        XWikiDocument modifiedDocument = getModifiedDocument(request, isDeletion, context);
        DocumentReference documentReference = modifiedDocument.getDocumentReferenceWithLocale();

        String title = request.getParameter("crTitle");
        String description = request.getParameter("crDescription");
        boolean isDraft = "1".equals(request.getParameter("crDraft"));

        UserReference currentUser = this.userReferenceResolver.resolve(CurrentUserReference.INSTANCE);
        ChangeRequest changeRequest = new ChangeRequest();

        String previousVersion = request.getParameter("previousVersion");
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
        FileChange.FileChangeType fileChangeType;
        if (isDeletion) {
            fileChangeType = FileChange.FileChangeType.DELETION;
        } else if (modifiedDocument.isNew()) {
            fileChangeType = FileChange.FileChangeType.CREATION;
        } else {
            fileChangeType = FileChange.FileChangeType.EDITION;
        }
        Date previousVersionDate = (previousDoc != null) ? previousDoc.getDate() : new Date();
        FileChange fileChange =
            getFileChange(changeRequest, fileChangeType, documentReference, modifiedDocument, previousVersion,
                previousVersionDate);

        changeRequest
            .setTitle(title)
            .setDescription(description)
            .setCreator(currentUser)
            .addFileChange(fileChange);

        if (isDraft) {
            changeRequest.setStatus(ChangeRequestStatus.DRAFT);
        } else {
            changeRequest.setStatus(ChangeRequestStatus.READY_FOR_REVIEW);
        }

        return changeRequest;
    }

    private FileChange getFileChange(ChangeRequest changeRequest, FileChange.FileChangeType fileChangeType,
        DocumentReference documentReference, XWikiDocument modifiedDocument, String requestPreviousVersion,
        Date previousVersionDate)
        throws ChangeRequestException
    {
        FileChange fileChange = new FileChange(changeRequest, fileChangeType);
        String fileChangeVersion;
        switch (fileChangeType) {
            case CREATION:
                fileChange
                    .setModifiedDocument(modifiedDocument);
                fileChangeVersion = this.fileChangeVersionManager.getNextFileChangeVersion("", false);
                break;

            case DELETION:
                XWikiContext context = contextProvider.get();
                try {
                    XWikiDocument document = context.getWiki().getDocument(documentReference, context);
                    String previousVersion = document.getVersion();
                    fileChange
                        .setPreviousVersion(previousVersion)
                        .setPreviousPublishedVersion(previousVersion, document.getDate());
                    fileChangeVersion = this.fileChangeVersionManager
                        .getNextFileChangeVersion(fileChange.getPreviousVersion(), false);
                } catch (XWikiException e) {
                    throw new ChangeRequestException("Cannot access the document for which a deletion request is "
                        + "performed.", e);
                }
                break;

            default:
            case EDITION:
                fileChange
                    .setPreviousVersion(requestPreviousVersion)
                    .setPreviousPublishedVersion(requestPreviousVersion, previousVersionDate)
                    .setModifiedDocument(modifiedDocument);
                fileChangeVersion = this.fileChangeVersionManager
                    .getNextFileChangeVersion(fileChange.getPreviousVersion(), false);
                break;
        }

        UserReference currentUser = this.userReferenceResolver.resolve(CurrentUserReference.INSTANCE);

        fileChange
            .setTargetEntity(documentReference)
            .setVersion(fileChangeVersion)
            .setAuthor(currentUser);
        return fileChange;
    }

    private void copyApprovers(ChangeRequest changeRequest)
        throws ChangeRequestException
    {
        Set<UserReference> usersApprovers = new HashSet<>();
        Set<DocumentReference> groupsApprovers = new HashSet<>();
        for (DocumentReference originalReference : changeRequest.getModifiedDocuments()) {
            usersApprovers.addAll(this.documentReferenceApproversManager.getAllApprovers(originalReference, false));
            groupsApprovers.addAll(this.documentReferenceApproversManager.getGroupsApprovers(originalReference));
        }

        if (!groupsApprovers.isEmpty()) {
            this.changeRequestApproversManager.setGroupsApprovers(groupsApprovers, changeRequest);
        }
        if (!usersApprovers.isEmpty()) {
            this.changeRequestApproversManager.setUsersApprovers(usersApprovers, changeRequest);
        }
    }
}
