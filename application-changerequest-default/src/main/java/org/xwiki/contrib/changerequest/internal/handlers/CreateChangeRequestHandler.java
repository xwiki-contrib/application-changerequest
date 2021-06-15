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
import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestReference;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.EditForm;

/**
 * Specific handler for creating a new change request.
 *
 * @version $Id$
 * @since 0.1
 */
@Component(roles = CreateChangeRequestHandler.class)
@Singleton
public class CreateChangeRequestHandler
{
    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private ChangeRequestStorageManager storageManager;

    @Inject
    private UserReferenceResolver<CurrentUserReference> userReferenceResolver;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private DocumentReferenceResolver<ChangeRequest> changeRequestDocumentReferenceResolver;

    /**
     * Handle the given {@link ChangeRequestReference} for performing the create.
     * @param changeRequestReference the request reference leading to this.
     * @throws ChangeRequestException in case of errors.
     */
    public void handle(ChangeRequestReference changeRequestReference) throws ChangeRequestException
    {
        XWikiContext context = this.contextProvider.get();
        HttpServletRequest request = context.getRequest();
        EditForm editForm = new EditForm();
        editForm.setRequest(request);
        editForm.readRequest();
        String serializedDocReference = request.getParameter("docReference");
        DocumentReference documentReference = this.documentReferenceResolver.resolve(serializedDocReference);
        String title = request.getParameter("crTitle");
        String description = request.getParameter("crDescription");

        UserReference currentUser = this.userReferenceResolver.resolve(CurrentUserReference.INSTANCE);
        ChangeRequest changeRequest = new ChangeRequest();
        FileChange fileChange = new FileChange(changeRequest);
        fileChange
            .setAuthor(currentUser)
            .setTargetEntity(documentReference)
            .setSourceVersion(request.getParameter("previousVersion"))
            .setContentChange(editForm.getContent());

        changeRequest
            .setTitle(title)
            .setDescription(description)
            .setCreator(currentUser)
            .setFileChanges(Collections.singletonList(fileChange))
            .setImpactedFiles(Collections.singletonList(documentReference));

        this.storageManager.save(changeRequest);

        DocumentReference changeRequestDocumentReference =
            this.changeRequestDocumentReferenceResolver.resolve(changeRequest);
        String url = context.getWiki().getURL(changeRequestDocumentReference, context);
        try {
            context.getResponse().sendRedirect(url);
        } catch (IOException e) {
            throw new ChangeRequestException("Error while redirecting to the created change request.", e);
        }
    }
}
