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

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ApproversManager;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestConfiguration;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.FileChangeSavingChecker;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Checkers ensuring that it's not possible to add changes to a change request that does not respect the minimum of
 * explicit approvers.
 *
 * @version $Id$
 * @since 0.16
 */
@Component
@Named("org.xwiki.contrib.changerequest.internal.checkers.MinimumApproversSavingChecker")
@Singleton
public class MinimumApproversSavingChecker implements FileChangeSavingChecker
{
    private static final String FAILURE_REASON = "changerequest.checkers.minimumApprovers.incompatibilityReason";

    @Inject
    private ChangeRequestConfiguration configuration;

    @Inject
    private ApproversManager<ChangeRequest> changeRequestApproversManager;

    @Inject
    private ApproversManager<XWikiDocument> documentApproversManager;

    @Inject
    private Logger logger;

    @Override
    public SavingCheckerResult canChangeOnDocumentBeAdded(ChangeRequest changeRequest,
        DocumentReference documentReference, FileChange.FileChangeType changeType)
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
        if (result) {
            return new SavingCheckerResult();
        } else {
            return new SavingCheckerResult(FAILURE_REASON);
        }
    }

    @Override
    public SavingCheckerResult canChangeOnDocumentBeAdded(ChangeRequest changeRequest, FileChange fileChange)
    {
        SavingCheckerResult result =
            this.canChangeOnDocumentBeAdded(changeRequest, fileChange.getTargetEntity(), fileChange.getType());
        // If the result is false, we're checking if the change is not an update of an existing filechange to add new
        // approvers. If the change increase the number of approvers then we should accept it, even if the total number
        // is not reached.
        if (!result.canBeSaved()) {
            Optional<FileChange> previousFileChangeOpt =
                changeRequest.getLatestFileChangeFor(fileChange.getTargetEntity());
            if (previousFileChangeOpt.isPresent()) {
                result = this.canChangeOnDocumentBeAdded(fileChange, previousFileChangeOpt.get());
            }
        }
        return result;
    }

    private SavingCheckerResult canChangeOnDocumentBeAdded(FileChange fileChange, FileChange previousFileChange)
    {
        SavingCheckerResult result = new SavingCheckerResult(FAILURE_REASON);
        DocumentModelBridge modifiedDocument = fileChange.getModifiedDocument();
        DocumentModelBridge previousModifiedDocument = previousFileChange.getModifiedDocument();
        if (modifiedDocument != null && previousModifiedDocument != null) {
            try {
                int minimumApprovers = configuration.getMinimumApprovers();
                int previousNumberApprovers = this.documentApproversManager
                    .getAllApprovers((XWikiDocument) previousModifiedDocument, false).size();
                int numberApprovers = this.documentApproversManager
                    .getAllApprovers((XWikiDocument) modifiedDocument, false).size();
                if (numberApprovers >= minimumApprovers || previousNumberApprovers <= numberApprovers) {
                    result = new SavingCheckerResult();
                } else {
                    result = new SavingCheckerResult(FAILURE_REASON);
                }
            } catch (ChangeRequestException e) {
                this.logger.warn("Error while trying to retrieve the approvers of filechange [{}]: [{}]",
                    fileChange,
                    ExceptionUtils.getRootCauseMessage(e));
            }
        }
        return result;
    }

    @Override
    public SavingCheckerResult canChangeRequestBeCreatedWith(FileChange fileChange)
    {
        boolean result = true;
        int minimumApprovers = configuration.getMinimumApprovers();
        if (minimumApprovers > 0 && fileChange.getModifiedDocument() != null) {
            try {
                int numberApprovers = this.documentApproversManager
                    .getAllApprovers((XWikiDocument) fileChange.getModifiedDocument(), false).size();
                result = numberApprovers >= minimumApprovers;
            } catch (ChangeRequestException e) {
                this.logger.warn("Error while trying to retrieve the approvers of filechange [{}] for creation check: "
                        + "[{}]",
                    fileChange,
                    ExceptionUtils.getRootCauseMessage(e));
            }
        }
        if (result) {
            return new SavingCheckerResult();
        } else {
            return new SavingCheckerResult(FAILURE_REASON);
        }
    }
}
