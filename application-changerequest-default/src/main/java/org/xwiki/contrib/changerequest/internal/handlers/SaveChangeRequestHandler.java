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
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.container.servlet.ServletResponse;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestReference;
import org.xwiki.contrib.changerequest.ChangeRequestStatus;
import org.xwiki.contrib.changerequest.events.ChangeRequestUpdatedEvent;
import org.xwiki.csrf.CSRFToken;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.wysiwyg.converter.HTMLConverter;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.EditForm;

/**
 * Component in charge of saving change request description or title.
 *
 * @version $Id$
 * @since 0.6
 */
@Component
@Named("save")
@Singleton
public class SaveChangeRequestHandler extends AbstractChangeRequestActionHandler
{
    private static final String CONTENT_PARAMETER = "content";

    private static final String REQUIRES_HTML_CONVERSION_PARAMETER = "RequiresHTMLConversion";

    private static final String CONTENT_SYNTAX_PARAMETER = "content_syntax";

    private static final String STATUS_PARAMETER = "status";

    private static final String AUTHORIZATION_ERROR = "You don't have right to edit this change request";

    private static final String TITLE_PARAMETER = "new-title";

    private static final String CHANGE_TYPE_PARAMETER = "changeType";

    @Inject
    private HTMLConverter htmlConverter;

    @Inject
    private CSRFToken csrfToken;

    @Inject
    private Container container;

    @Override
    public void handle(ChangeRequestReference changeRequestReference)
        throws ChangeRequestException, IOException
    {
        HttpServletRequest request =
            ((ServletRequest) this.container.getRequest()).getHttpServletRequest();
        HttpServletResponse response =
            ((ServletResponse) this.container.getResponse()).getHttpServletResponse();
        if (!this.csrfToken.isTokenValid(request.getParameter("form_token"))) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Wrong CSRF token");
        } else {
            ChangeRequest changeRequest = this.loadChangeRequest(changeRequestReference);
            boolean success;
            if ("GET".equals(request.getMethod())) {
                success = this.handleStatusUpdate(request, response, changeRequest);
            } else {
                success = this.handleDescriptionOrTitleUpdate(request, response, changeRequest);
            }
            if (success) {
                this.responseSuccess(changeRequest);
            }
        }
    }

    private boolean handleStatusUpdate(HttpServletRequest request, HttpServletResponse response,
        ChangeRequest changeRequest) throws IOException, ChangeRequestException
    {
        String statusParameter = request.getParameter(STATUS_PARAMETER);
        boolean result = false;
        try {
            ChangeRequestStatus changeRequestStatus = ChangeRequestStatus.valueOf(statusParameter.toUpperCase());
            boolean isReopenAuthorized = isReopenStatusTransitionAuthorized(changeRequest.getStatus(),
                changeRequestStatus)
                && this.changeRequestRightsManager.isAuthorizedToOpen(getCurrentUser(), changeRequest);
            if (!this.changeRequestRightsManager.isAuthorizedToEdit(getCurrentUser(), changeRequest)
                && !isReopenAuthorized) {
                response
                    .sendError(HttpServletResponse.SC_FORBIDDEN, AUTHORIZATION_ERROR);
            } else {
                this.changeRequestManager.updateStatus(changeRequest, changeRequestStatus);
                result = true;
            }
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                String.format("Wrong status parameter: [%s]", statusParameter));
        }
        return result;
    }

    private boolean isReopenStatusTransitionAuthorized(ChangeRequestStatus currentStatus,
        ChangeRequestStatus nextStatus)
    {
        return (currentStatus == ChangeRequestStatus.CLOSED || currentStatus == ChangeRequestStatus.STALE)
            && (nextStatus == ChangeRequestStatus.READY_FOR_REVIEW || nextStatus == ChangeRequestStatus.DRAFT);
    }

    private boolean handleDescriptionOrTitleUpdate(HttpServletRequest request, HttpServletResponse response,
        ChangeRequest changeRequest) throws ChangeRequestException, IOException
    {
        if (this.changeRequestRightsManager.isAuthorizedToEdit(getCurrentUser(), changeRequest)) {
            String changeType = request.getParameter(CHANGE_TYPE_PARAMETER);

            if (StringUtils.equals(changeType, "settitle")) {
                String title = request.getParameter(TITLE_PARAMETER);
                changeRequest.setTitle(title);
                this.storageManager.save(changeRequest);
                this.observationManager.notify(new ChangeRequestUpdatedEvent(), changeRequest.getId(), changeRequest);
            } else {
                String content = getContent(request);
                changeRequest.setDescription(content);

                EditForm editForm = this.prepareForm(request);
                DocumentReference documentReference =
                    this.changeRequestDocumentReferenceResolver.resolve(changeRequest);
                XWikiContext context = this.contextProvider.get();

                XWikiDocument document = null;
                try {
                    document = context.getWiki().getDocument(documentReference, context);
                    document.readTemporaryUploadedFiles(editForm);
                } catch (XWikiException e) {
                    throw new ChangeRequestException(
                        "Error while loading the change request document for reading uploaded files", e);
                }

                this.storageManager.save(changeRequest);
                this.observationManager.notify(new ChangeRequestUpdatedEvent(), changeRequest.getId(), changeRequest);
            }
            return true;
        } else {
            response
                .sendError(HttpServletResponse.SC_FORBIDDEN, AUTHORIZATION_ERROR);
            return false;
        }
    }

    // Code inspired from DiscussionsResourceReferenceHandler
    private String getContent(HttpServletRequest request)
    {
        String content = request.getParameter(CONTENT_PARAMETER);
        String requiresHTMLConversion = request.getParameter(REQUIRES_HTML_CONVERSION_PARAMETER);
        String syntax = request.getParameter(CONTENT_SYNTAX_PARAMETER);

        String contentClean;
        if (Objects.equals(requiresHTMLConversion, CONTENT_PARAMETER)) {
            contentClean = this.htmlConverter.fromHTML(content, syntax);
        } else {
            contentClean = content;
        }
        return contentClean;
    }
}
