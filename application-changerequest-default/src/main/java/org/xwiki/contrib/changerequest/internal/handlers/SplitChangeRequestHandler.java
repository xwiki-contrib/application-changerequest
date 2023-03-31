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
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.container.servlet.ServletResponse;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestReference;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.csrf.CSRFToken;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.XWikiContext;

/**
 * Handler responsible of splitting change requests.
 *
 * @version $Id$
 * @since 1.6
 */
@Component
@Named("split")
@Singleton
public class SplitChangeRequestHandler extends AbstractChangeRequestActionHandler
{
    private static final LocalDocumentReference CHANGE_REQUEST_HOME =
        new LocalDocumentReference("ChangeRequest", "WebHome");

    @Inject
    private CSRFToken csrfToken;

    @Inject
    private ChangeRequestStorageManager changeRequestStorageManager;

    @Override
    public void handle(ChangeRequestReference changeRequestReference) throws ChangeRequestException, IOException
    {
        ChangeRequest changeRequest = this.loadChangeRequest(changeRequestReference);
        HttpServletResponse response =
            ((ServletResponse) this.container.getResponse()).getHttpServletResponse();
        HttpServletRequest request =
            ((ServletRequest) this.container.getRequest()).getHttpServletRequest();

        if (!this.csrfToken.isTokenValid(request.getParameter("form_token"))) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Wrong CSRF token");
        } else if (!StringUtils.equals(request.getParameter("confirm"), "1")) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing confirm");
        } else if (!this.changeRequestRightsManager.isAuthorizedToSplit(getCurrentUser(), changeRequest)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing authorization to split");
        } else {
            this.changeRequestStorageManager.split(changeRequest);
            XWikiContext context = this.contextProvider.get();
            String crHomeURL = context.getWiki().getURL(CHANGE_REQUEST_HOME, context);
            response.sendRedirect(crHomeURL);
        }
    }
}
