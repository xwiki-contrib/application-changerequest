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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.container.Container;
import org.xwiki.contrib.changerequest.ApproversManager;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestConfiguration;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestManager;
import org.xwiki.contrib.changerequest.ChangeRequestReference;
import org.xwiki.contrib.changerequest.ChangeRequestRightsManager;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.internal.FileChangeVersionManager;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.contrib.changerequest.storage.FileChangeStorageManager;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.observation.ObservationManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.wysiwyg.converter.RequestParameterConverter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.EditForm;
import com.xpn.xwiki.web.XWikiResponse;

/**
 * Abstract implementation of {@link ChangeRequestActionHandler} which provides some utility methods.
 *
 * @version $Id$
 * @since 0.3
 */
public abstract class AbstractChangeRequestActionHandler implements ChangeRequestActionHandler
{
    static final String PREVIOUS_VERSION_PARAMETER = "previousVersion";
    static final String FROM_CHANGE_REQUEST_PARAMETER = "fromchangerequest";
    private static final String ASYNC_PARAMETER = "async";

    @Inject
    protected Provider<XWikiContext> contextProvider;

    @Inject
    protected DocumentReferenceResolver<ChangeRequest> changeRequestDocumentReferenceResolver;

    @Inject
    protected ChangeRequestStorageManager storageManager;

    @Inject
    protected ObservationManager observationManager;

    @Inject
    @Named("current")
    protected DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    protected FileChangeStorageManager fileChangeStorageManager;

    @Inject
    protected Container container;

    @Inject
    protected FileChangeVersionManager fileChangeVersionManager;

    @Inject
    protected ChangeRequestRightsManager changeRequestRightsManager;

    @Inject
    protected ChangeRequestManager changeRequestManager;

    @Inject
    private RequestParameterConverter requestParameterConverter;

    @Inject
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @Inject
    @Named("currentmixed")
    private DocumentReferenceResolver<String> currentMixedDocumentReferenceResolver;

    @Inject
    private UserReferenceResolver<CurrentUserReference> currentUserReferenceResolver;

    @Inject
    private ApproversManager<FileChange> fileChangeApproversManager;

    @Inject
    private ApproversManager<ChangeRequest> changeRequestApproversManager;

    @Inject
    private ChangeRequestConfiguration configuration;

    @Inject
    private ContextualLocalizationManager contextualLocalizationManager;

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
        this.redirectToChangeRequest(changeRequest, "view");
    }

    protected void redirectToChangeRequest(ChangeRequest changeRequest, String action) throws IOException
    {
        XWikiContext context = this.contextProvider.get();
        DocumentReference changeRequestDocumentReference =
            this.changeRequestDocumentReferenceResolver.resolve(changeRequest);
        String url = context.getWiki().getURL(changeRequestDocumentReference, action, context);
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

    protected boolean isFromChangeRequest(HttpServletRequest request)
    {
        return StringUtils.equals(request.getParameter(FROM_CHANGE_REQUEST_PARAMETER), "1");
    }

    protected String getPreviousVersion(HttpServletRequest request)
    {
        String previousVersionParameter = request.getParameter(PREVIOUS_VERSION_PARAMETER);
        if (isFromChangeRequest(request)) {
            return this.fileChangeVersionManager.getFileChangeVersion(previousVersionParameter);
        } else {
            return previousVersionParameter;
        }
    }

    protected XWikiDocument prepareDocument(HttpServletRequest request, EditForm editForm, ChangeRequest changeRequest)
        throws ChangeRequestException
    {
        XWikiContext context = this.contextProvider.get();
        String serializedDocReference = request.getParameter("docReference");
        DocumentReference documentReference = this.documentReferenceResolver.resolve(serializedDocReference);
        UserReference currentUserReference = this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE);
        if (!this.changeRequestRightsManager.isEditWithChangeRequestAllowed(currentUserReference, documentReference)) {
            throw new ChangeRequestException(
                String.format("User [%s] is not allowed to edit the document [%s] through a change request.",
                    currentUserReference, documentReference));
        }

        XWikiDocument modifiedDocument = null;
        try {
            if (isFromChangeRequest(request) && changeRequest != null) {
                List<FileChange> fileChangeList = this.fileChangeStorageManager.load(changeRequest, documentReference);
                String previousVersion = getPreviousVersion(request);
                FileChange fileChange = null;
                for (FileChange change : fileChangeList) {
                    if (change.getVersion().equals(previousVersion)) {
                        fileChange = change;
                        break;
                    }
                }
                if (fileChange != null) {
                    modifiedDocument = (XWikiDocument) fileChange.getModifiedDocument();
                } else {
                    throw new ChangeRequestException(
                        String.format("Cannot find file change with version [%s]", previousVersion));
                }
            } else {
                modifiedDocument = context.getWiki().getDocument(documentReference, context);
            }
            // cloning the document to ensure we don't impact the document in cache.
            modifiedDocument = modifiedDocument.clone();

            // Read info from the template if there's one.
            if (!StringUtils.isBlank(editForm.getTemplate())) {
                DocumentReference templateRef =
                    this.currentMixedDocumentReferenceResolver.resolve(editForm.getTemplate());

                // Check that the template can be read by current user.
                if (this.contextualAuthorizationManager.hasAccess(Right.VIEW, templateRef)) {
                    modifiedDocument.readFromTemplate(templateRef, context);
                }
            }

            modifiedDocument.readFromForm(editForm, context);
            if (modifiedDocument.getDefaultLocale() == Locale.ROOT) {
                modifiedDocument.setDefaultLocale(context.getWiki().getLocalePreference(context));
            }
            return modifiedDocument;
        } catch (XWikiException e) {
            throw new ChangeRequestException(
                String.format("Cannot read document [%s]", serializedDocReference), e);
        }
    }

    protected void responseSuccess(ChangeRequest changeRequest) throws IOException
    {
        XWikiContext context = this.contextProvider.get();
        if (this.isAsync(context.getRequest())) {
            Map<String, String> json = new HashMap<>();
            json.put("changeRequestId", changeRequest.getId());
            DocumentReference changeRequestRef = this.changeRequestDocumentReferenceResolver.resolve(changeRequest);
            json.put("changeRequestUrl", context.getWiki().getURL(changeRequestRef, context));
            this.answerJSON(HttpServletResponse.SC_OK, json);
        } else {
            this.redirectToChangeRequest(changeRequest);
        }
    }

    protected boolean isAsync(HttpServletRequest request)
    {
        return "1".equals(request.getParameter(ASYNC_PARAMETER));
    }

    /**
     * Answer to a request with a JSON content.
     * Note: this method was partially copied from {@link com.xpn.xwiki.web.XWikiAction}.
     *
     * @param status the status code to send back.
     * @param answer the content of the JSON answer.
     * @throws IOException in case of error during the serialization of the JSON.
     */
    protected void answerJSON(int status, Map<String, String> answer) throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        XWikiContext context = contextProvider.get();
        XWikiResponse response = context.getResponse();
        String jsonAnswerAsString = mapper.writeValueAsString(answer);
        String encoding = context.getWiki().getEncoding();
        response.setContentType("application/json");
        // Set the content length to the number of bytes, not the
        // string length, so as to handle multi-byte encodings
        response.setContentLength(jsonAnswerAsString.getBytes(encoding).length);
        response.setStatus(status);
        response.setCharacterEncoding(encoding);
        response.getWriter().print(jsonAnswerAsString);
        context.setResponseSent(true);
    }

    protected UserReference getCurrentUser()
    {
        return this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE);
    }

    protected void copyApprovers(FileChange fileChange) throws ChangeRequestException
    {
        ChangeRequest changeRequest = fileChange.getChangeRequest();

        // Existing approvers of the change request.
        Set<UserReference> usersCRApprovers =
            new LinkedHashSet<>(this.changeRequestApproversManager.getAllApprovers(changeRequest, false));
        Set<DocumentReference> groupsCRApprovers =
            new LinkedHashSet<>(this.changeRequestApproversManager.getGroupsApprovers(changeRequest));

        usersCRApprovers.addAll(this.fileChangeApproversManager.getAllApprovers(fileChange, false));
        groupsCRApprovers.addAll(this.fileChangeApproversManager.getGroupsApprovers(fileChange));

        // If the authored are prevented to review, we ensure to remove it from the list of users approvers.
        if (this.configuration.preventAuthorToReview()) {
            usersCRApprovers.remove(fileChange.getAuthor());
        }

        if (!groupsCRApprovers.isEmpty()) {
            this.changeRequestApproversManager.setGroupsApprovers(groupsCRApprovers, changeRequest);
        }
        if (!usersCRApprovers.isEmpty()) {
            this.changeRequestApproversManager.setUsersApprovers(usersCRApprovers, changeRequest);
        }
    }

    protected void reportError(int statusCode, String localizationKey, Object... parameters) throws IOException
    {
        this.answerJSON(statusCode, Collections.singletonMap("changeRequestError",
            this.contextualLocalizationManager.getTranslationPlain(localizationKey, parameters)));
    }
}
