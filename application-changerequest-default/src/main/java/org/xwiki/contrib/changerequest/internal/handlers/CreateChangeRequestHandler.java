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
import java.util.Locale;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ApproversManager;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestReference;
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

    /**
     * Handle the given {@link ChangeRequestReference} for performing the create.
     * @param changeRequestReference the request reference leading to this.
     * @throws ChangeRequestException in case of errors.
     */
    @Override
    public void handle(ChangeRequestReference changeRequestReference) throws ChangeRequestException, IOException
    {
        HttpServletRequest request = this.prepareRequest();
        boolean isDeletion = "1".equals(request.getParameter("deletion"));

        XWikiDocument modifiedDocument = null;
        DocumentReference documentReference;

        if (!isDeletion) {
            EditForm editForm = this.prepareForm(request);
            modifiedDocument = this.prepareDocument(request, editForm);
            documentReference = modifiedDocument.getDocumentReferenceWithLocale();
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
            documentReference = new DocumentReference(referenceWithoutLocale, locale);
        }

        String title = request.getParameter("crTitle");
        String description = request.getParameter("crDescription");
        boolean isDraft = "1".equals(request.getParameter("crDraft"));

        UserReference currentUser = this.userReferenceResolver.resolve(CurrentUserReference.INSTANCE);
        ChangeRequest changeRequest = new ChangeRequest();

        String previousVersion = request.getParameter("previousVersion");
        FileChange fileChange =
            getFileChange(changeRequest, isDeletion, documentReference, modifiedDocument, previousVersion);

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

        this.storageManager.save(changeRequest);
        this.copyApprovers(documentReference, changeRequest);
        this.observationManager.notify(new ChangeRequestCreatedEvent(), changeRequest.getId(), changeRequest);
        this.redirectToChangeRequest(changeRequest);
    }

    private FileChange getFileChange(ChangeRequest changeRequest, boolean isDeletion,
        DocumentReference documentReference, XWikiDocument modifiedDocument, String requestPreviousVersion)
        throws ChangeRequestException
    {
        FileChange fileChange;
        if (isDeletion) {
            fileChange = new FileChange(changeRequest, FileChange.FileChangeType.DELETION);
            XWikiContext context = contextProvider.get();
            try {
                XWikiDocument document = context.getWiki().getDocument(documentReference, context);
                String previousVersion = document.getVersion();
                fileChange
                    .setPreviousVersion(previousVersion)
                    .setPreviousPublishedVersion(previousVersion);
            } catch (XWikiException e) {
                throw new
                    ChangeRequestException("Cannot access the document for which a deletion request is performed.", e);
            }
        } else {
            fileChange = new FileChange(changeRequest);
            fileChange
                .setPreviousVersion(requestPreviousVersion)
                .setPreviousPublishedVersion(requestPreviousVersion)
                .setModifiedDocument(modifiedDocument);
        }
        UserReference currentUser = this.userReferenceResolver.resolve(CurrentUserReference.INSTANCE);
        String fileChangeVersion = this.fileChangeVersionManager
            .getNextFileChangeVersion(fileChange.getPreviousVersion(), false);
        fileChange
            .setTargetEntity(documentReference)
            .setVersion(fileChangeVersion)
            .setAuthor(currentUser);
        return fileChange;
    }

    private void copyApprovers(DocumentReference originalReference, ChangeRequest changeRequest)
        throws ChangeRequestException
    {
        Set<UserReference> usersApprovers =
            this.documentReferenceApproversManager.getAllApprovers(originalReference, false);
        Set<DocumentReference> groupsApprovers =
            this.documentReferenceApproversManager.getGroupsApprovers(originalReference);

        if (!groupsApprovers.isEmpty()) {
            this.changeRequestApproversManager.setGroupsApprovers(groupsApprovers, changeRequest);
        }
        if (!usersApprovers.isEmpty()) {
            this.changeRequestApproversManager.setUsersApprovers(usersApprovers, changeRequest);
        }
    }
}
