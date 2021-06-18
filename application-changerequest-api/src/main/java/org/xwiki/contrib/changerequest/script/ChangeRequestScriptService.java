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
package org.xwiki.contrib.changerequest.script;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestManager;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

/**
 * Script service for change request.
 *
 * @version $Id$
 * @since 0.1-SNAPSHOT
 */
@Unstable
@Component
@Named("changerequest")
@Singleton
public class ChangeRequestScriptService implements ScriptService
{
    @Inject
    private ChangeRequestManager changeRequestManager;

    @Inject
    private ChangeRequestStorageManager changeRequestStorageManager;

    @Inject
    private UserReferenceResolver<CurrentUserReference> currentUserReferenceResolver;

    @Inject
    private Logger logger;

    /**
     * Retrieve the change request identified with the given id and check if it can be merged.
     * This method checks if the approval strategy is reached, if the current user is authorized to perform the merge,
     * and if the change request has conflicts.
     *
     * @param changeRequestId the identifier of the change request to check.
     * @return {@code true} if a change request matching the id is found and can be merged (i.e. the approval strategy
     *          allows it, the user is authorized to do it, and the change request does not have conflicts).
     */
    public boolean canBeMerged(String changeRequestId)
    {
        boolean result = false;
        try {
            Optional<ChangeRequest> changeRequestOpt = this.changeRequestStorageManager.load(changeRequestId);
            if (changeRequestOpt.isPresent()) {
                UserReference currentUser = this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE);
                ChangeRequest changeRequest = changeRequestOpt.get();
                result = this.changeRequestManager.isAuthorizedToMerge(currentUser, changeRequest)
                    && this.changeRequestManager.canBeMerged(changeRequest);
            } else {
                this.logger.warn("Cannot find change request with id [{}].", changeRequestId);
            }
        } catch (ChangeRequestException e) {
            this.logger.warn("Error while checking if the change request [{}] can be merged: [{}]",
                changeRequestId, ExceptionUtils.getRootCauseMessage(e));
        }
        return result;
    }
}
