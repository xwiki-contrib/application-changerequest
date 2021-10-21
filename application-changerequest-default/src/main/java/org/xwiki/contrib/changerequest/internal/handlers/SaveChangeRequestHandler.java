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

import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.container.servlet.ServletResponse;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestReference;
import org.xwiki.csrf.CSRFToken;
import org.xwiki.wysiwyg.converter.HTMLConverter;

/**
 * Component in charge of saving change request description.
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
            String content = getContent(request);
            changeRequest.setDescription(content);
            this.storageManager.save(changeRequest);
            this.redirectToChangeRequest(changeRequest);
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
