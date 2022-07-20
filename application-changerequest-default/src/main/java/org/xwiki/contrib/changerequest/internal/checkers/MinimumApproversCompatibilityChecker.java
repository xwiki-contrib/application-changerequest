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
package org.xwiki.contrib.changerequest.internal.checkers;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ApproversManager;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestConfiguration;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.FileChangeCompatibilityChecker;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;

/**
 * Checkers ensuring that it's not possible to add changes to a change request that does not respect the minimum of
 * explicit approvers.
 *
 * @version $Id$
 * @since 0.16
 */
@Component
@Named("MinimumApproversCompatibilityChecker")
@Singleton
public class MinimumApproversCompatibilityChecker implements FileChangeCompatibilityChecker
{
    @Inject
    private ChangeRequestConfiguration configuration;

    @Inject
    private ApproversManager<ChangeRequest> changeRequestApproversManager;

    @Inject
    private ContextualLocalizationManager contextualLocalizationManager;

    @Inject
    private Logger logger;

    @Override
    public boolean canChangeOnDocumentBeAdded(ChangeRequest changeRequest, DocumentReference documentReference,
        FileChange.FileChangeType changeType)
    {
        boolean result = true;
        int minimumApprovers = configuration.getMinimumApprovers();
        if (minimumApprovers > 0) {
            try {
                result =
                    minimumApprovers <= this.changeRequestApproversManager.getAllApprovers(changeRequest, false).size();
            } catch (ChangeRequestException e) {
                this.logger.warn("Error while trying to retrieve the approvers of change request [{}]: [{}]",
                    changeRequest,
                    ExceptionUtils.getRootCauseMessage(e));
            }
        }
        return result;
    }

    @Override public String getIncompatibilityReason(ChangeRequest changeRequest, DocumentReference documentReference,
        FileChange.FileChangeType changeType)
    {
        return contextualLocalizationManager
            .getTranslationPlain("changerequest.checkers.minimumApprovers.incompatibilityReason");
    }
}
