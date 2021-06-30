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
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestManager;
import org.xwiki.contrib.changerequest.ChangeRequestReference;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWikiContext;

/**
 * Component responsible to handle a merge request.
 *
 * @version $Id$
 * @since 0.1
 */
@Component(roles = MergeChangeRequestHandler.class)
@Singleton
public class MergeChangeRequestHandler
{
    @Inject
    private ChangeRequestManager changeRequestManager;

    @Inject
    private ChangeRequestStorageManager changeRequestStorageManager;

    @Inject
    private UserReferenceResolver<CurrentUserReference> userReferenceResolver;

    @Inject
    private DocumentReferenceResolver<ChangeRequest> changeRequestDocumentReferenceResolver;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private Logger logger;

    /**
     * Handling method. (Dumb javadoc since it's supposed to be inherited)
     *
     * @param changeRequestReference the change request reference.
     * @throws ChangeRequestException in case of errors when handling the request
     */
    public void handle(ChangeRequestReference changeRequestReference) throws ChangeRequestException
    {
        Optional<ChangeRequest> changeRequestOpt = this.changeRequestStorageManager
            .load(changeRequestReference.getId());
        if (changeRequestOpt.isPresent()) {
            ChangeRequest changeRequest = changeRequestOpt.get();
            UserReference currentUser = this.userReferenceResolver.resolve(CurrentUserReference.INSTANCE);
            if (this.changeRequestManager.isAuthorizedToMerge(currentUser, changeRequest)
                && this.changeRequestManager.canBeMerged(changeRequest)) {
                this.changeRequestStorageManager.merge(changeRequest);
                DocumentReference changeRequestDocumentReference =
                    this.changeRequestDocumentReferenceResolver.resolve(changeRequest);
                XWikiContext context = this.contextProvider.get();
                String url = context.getWiki().getURL(changeRequestDocumentReference, context);
                try {
                    context.getResponse().sendRedirect(url);
                } catch (IOException e) {
                    throw new ChangeRequestException("Error while redirecting to the created change request.", e);
                }
            } else {
                this.logger.warn("The change request [{}] cannot be merged.", changeRequestReference.getId());
            }
        } else {
            this.logger.warn("Cannot find change request with id [{}]", changeRequestReference.getId());
        }
    }
}
