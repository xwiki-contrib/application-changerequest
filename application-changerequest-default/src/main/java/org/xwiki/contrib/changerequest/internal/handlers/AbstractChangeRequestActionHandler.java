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
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;

import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestReference;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.wysiwyg.converter.RequestParameterConverter;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.EditForm;

/**
 * Abstract implementation of {@link ChangeRequestActionHandler} which provides some utility methods.
 *
 * @version $Id$
 * @since 0.3
 */
public abstract class AbstractChangeRequestActionHandler implements ChangeRequestActionHandler
{
    @Inject
    protected Provider<XWikiContext> contextProvider;

    @Inject
    protected DocumentReferenceResolver<ChangeRequest> changeRequestDocumentReferenceResolver;

    @Inject
    protected ChangeRequestStorageManager storageManager;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private RequestParameterConverter requestParameterConverter;

    protected HttpServletRequest prepareRequest() throws ChangeRequestException
    {
        XWikiContext context = this.contextProvider.get();
        HttpServletRequest request = context.getRequest();
        try {
            return (HttpServletRequest) this.requestParameterConverter.convert(request, context.getResponse()).get();
        } catch (IOException e) {
            throw new ChangeRequestException("Error while converting request", e);
        }
    }

    protected EditForm prepareForm(HttpServletRequest request)
    {
        EditForm editForm = new EditForm();
        editForm.setRequest(request);
        editForm.readRequest();
        return editForm;
    }

    protected void redirectToChangeRequest(ChangeRequest changeRequest) throws IOException
    {
        XWikiContext context = this.contextProvider.get();
        DocumentReference changeRequestDocumentReference =
            this.changeRequestDocumentReferenceResolver.resolve(changeRequest);
        String url = context.getWiki().getURL(changeRequestDocumentReference, context);
        context.getResponse().sendRedirect(url);
    }

    protected ChangeRequest loadChangeRequest(ChangeRequestReference reference)
        throws ChangeRequestException, IOException
    {
        Optional<ChangeRequest> changeRequestOptional = this.storageManager.load(reference.getId());
        if (changeRequestOptional.isPresent()) {
            return changeRequestOptional.get();
        } else {
            XWikiContext context = this.contextProvider.get();
            context.getResponse().sendError(404, String.format("Cannot find change request with id [%s]",
                    reference.getId()));
        }
        return null;
    }

    protected XWikiDocument prepareDocument(HttpServletRequest request, EditForm editForm) throws ChangeRequestException
    {
        XWikiContext context = this.contextProvider.get();
        String serializedDocReference = request.getParameter("docReference");
        DocumentReference documentReference = this.documentReferenceResolver.resolve(serializedDocReference);
        try {
            XWikiDocument modifiedDocument = context.getWiki().getDocument(documentReference, context);
            // cloning the document to ensure we don't impact the document in cache.
            modifiedDocument = modifiedDocument.clone();
            modifiedDocument.readFromForm(editForm, context);
            return modifiedDocument;
        } catch (XWikiException e) {
            throw new ChangeRequestException(
                String.format("Cannot read document [%s]", serializedDocReference), e);
        }
    }
}
