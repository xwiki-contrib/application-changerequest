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

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestManager;
import org.xwiki.contrib.changerequest.ChangeRequestReference;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

/**
 * Component responsible to handle a merge request.
 *
 * @version $Id$
 * @since 0.1
 */
@Component
@Named("merge")
@Singleton
public class MergeChangeRequestHandler extends AbstractChangeRequestActionHandler
{
    @Inject
    private ChangeRequestManager changeRequestManager;

    @Inject
    private UserReferenceResolver<CurrentUserReference> userReferenceResolver;

    /**
     * Handling method. (Dumb javadoc since it's supposed to be inherited)
     *
     * @param changeRequestReference the change request reference.
     * @throws ChangeRequestException in case of errors when handling the request
     */
    @Override
    public void handle(ChangeRequestReference changeRequestReference) throws ChangeRequestException, IOException
    {
        ChangeRequest changeRequest = this.loadChangeRequest(changeRequestReference);
        if (changeRequest != null) {
            UserReference currentUser = this.userReferenceResolver.resolve(CurrentUserReference.INSTANCE);
            if (!this.changeRequestManager.isAuthorizedToMerge(currentUser, changeRequest)) {
                this.contextProvider.get().getResponse().sendError(403,
                    String.format("You're not authorized to merge change request [%s].",
                        changeRequestReference.getId()));
            } else if (!this.changeRequestManager.canBeMerged(changeRequest)) {
                this.contextProvider.get().getResponse().sendError(409,
                    String.format("The change request [%s] cannot be merged.",
                        changeRequestReference.getId()));
            } else {
                this.storageManager.merge(changeRequest);
                this.responseSuccess(changeRequest);
            }
        }
    }
}
