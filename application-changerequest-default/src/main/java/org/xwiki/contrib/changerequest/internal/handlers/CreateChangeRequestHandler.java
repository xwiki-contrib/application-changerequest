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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestReference;
import org.xwiki.contrib.changerequest.ChangeRequestStatus;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.events.ChangeRequestCreatedEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
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

    /**
     * Handle the given {@link ChangeRequestReference} for performing the create.
     * @param changeRequestReference the request reference leading to this.
     * @throws ChangeRequestException in case of errors.
     */
    @Override
    public void handle(ChangeRequestReference changeRequestReference) throws ChangeRequestException, IOException
    {
        HttpServletRequest request = this.prepareRequest();
        EditForm editForm = this.prepareForm(request);
        XWikiDocument modifiedDocument = this.prepareDocument(request, editForm);
        DocumentReference documentReference = modifiedDocument.getDocumentReferenceWithLocale();
        String title = request.getParameter("crTitle");
        String description = request.getParameter("crDescription");
        boolean isDraft = "1".equals(request.getParameter("crDraft"));

        UserReference currentUser = this.userReferenceResolver.resolve(CurrentUserReference.INSTANCE);
        ChangeRequest changeRequest = new ChangeRequest();
        FileChange fileChange = new FileChange(changeRequest);

        fileChange
            .setAuthor(currentUser)
            .setTargetEntity(documentReference)
            .setSourceVersion(request.getParameter("previousVersion"))
            .setModifiedDocument(modifiedDocument);

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

        this.observationManager.notify(new ChangeRequestCreatedEvent(), documentReference, changeRequest.getId());
        this.redirectToChangeRequest(changeRequest);
    }
}
