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

import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.container.servlet.ServletResponse;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestManager;
import org.xwiki.contrib.changerequest.ChangeRequestReference;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.storage.FileChangeStorageManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;

/**
 * Component responsible to handle a rebase request.
 *
 * @version $Id$
 * @since 0.6
 */
@Component
@Named("rebase")
@Singleton
public class RebaseChangeRequestHandler extends AbstractChangeRequestActionHandler
{
    private static final String REFERENCE_PARAMETER = "referenceParameter";
    private static final String LOCALE_PARAMETER = "locale";

    @Inject
    private FileChangeStorageManager fileChangeStorageManager;

    @Inject
    private ChangeRequestManager changeRequestManager;

    @Inject
    private Container container;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Override
    public void handle(ChangeRequestReference changeRequestReference)
        throws ChangeRequestException, IOException
    {
        ChangeRequest changeRequest = this.loadChangeRequest(changeRequestReference);
        HttpServletResponse response =
            ((ServletResponse) this.container.getResponse()).getHttpServletResponse();
        HttpServletRequest request =
            ((ServletRequest) this.container.getRequest()).getHttpServletRequest();
        String serializedReference = request.getParameter(REFERENCE_PARAMETER);

        boolean allFileChanges = StringUtils.isEmpty(serializedReference);

        DocumentReference reference = null;
        if (!allFileChanges) {
            reference = this.documentReferenceResolver.resolve(serializedReference);
            reference = new DocumentReference(reference, LocaleUtils.toLocale(request.getParameter(LOCALE_PARAMETER)));
        }
        if (changeRequest != null) {
            if (this.changeRequestManager.isAuthorizedToEdit(this.getCurrentUser(), changeRequest)) {
                for (FileChange lastFileChange : changeRequest.getLastFileChanges()) {
                    if (allFileChanges || lastFileChange.getTargetEntity().equals(reference)) {
                        this.fileChangeStorageManager.rebase(lastFileChange);
                    }
                }
                this.responseSuccess(changeRequest);
            } else {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "You are not authorized to perform a rebase.");
            }
        }
    }
}
