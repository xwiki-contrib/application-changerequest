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

import java.util.Set;

import javax.annotation.Priority;
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
import org.xwiki.contrib.changerequest.FileChangeSavingChecker;
import org.xwiki.contrib.changerequest.internal.UserReferenceConverter;
import org.xwiki.contrib.changerequest.rights.ChangeRequestApproveRight;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.user.UserReference;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Checker that ensures that the provided approvers have enough rights.
 *
 * @version $Id$
 * @since 1.7
 */
@Component
@Named("ApproversRightChecker")
@Singleton
@Priority(120)
public class ApproversRightChecker implements FileChangeSavingChecker
{
    @Inject
    private ChangeRequestConfiguration configuration;

    @Inject
    private ApproversManager<XWikiDocument> fileChangeApproversManager;

    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    private UserReferenceConverter userReferenceConverter;

    @Inject
    private Logger logger;

    @Override
    public SavingCheckerResult canChangeOnDocumentBeAdded(ChangeRequest changeRequest,
        DocumentReference documentReference, FileChange.FileChangeType changeType)
    {
        // We always allow the changes here as we cannot guess anything.
        return new SavingCheckerResult();
    }

    @Override
    public SavingCheckerResult canChangeOnDocumentBeAdded(ChangeRequest changeRequest, FileChange fileChange)
    {
        return this.checkCompatibility(fileChange);
    }

    @Override
    public SavingCheckerResult canChangeRequestBeCreatedWith(FileChange fileChange)
    {
        return this.checkCompatibility(fileChange);
    }

    private SavingCheckerResult checkCompatibility(FileChange fileChange)
    {
        SavingCheckerResult result = new SavingCheckerResult();
        if (this.configuration.acceptOnlyAllowedApprovers() && !this.areApproversAllowed(fileChange)) {
            result = new SavingCheckerResult("changerequest.checkers.approversright.incompatibilityReason");
        }
        return result;
    }

    private boolean areApproversAllowed(FileChange fileChange)
    {
        boolean result = true;
        if (fileChange.getType() == FileChange.FileChangeType.EDITION
            || fileChange.getType() == FileChange.FileChangeType.CREATION) {
            XWikiDocument modifiedDocument = (XWikiDocument) fileChange.getModifiedDocument();
            try {
                Right approveRight = ChangeRequestApproveRight.getRight();
                Set<UserReference> userApprovers =
                    this.fileChangeApproversManager.getAllApprovers(modifiedDocument, false);
                for (UserReference userApprover : userApprovers) {
                    DocumentReference userDoc = this.userReferenceConverter.convert(userApprover);
                    if (!this.authorizationManager.hasAccess(approveRight, userDoc,
                        modifiedDocument.getDocumentReference())) {
                        return false;
                    }
                }

                Set<DocumentReference> groupsApprovers =
                    this.fileChangeApproversManager.getGroupsApprovers(modifiedDocument);
                for (DocumentReference groupsApprover : groupsApprovers) {
                    if (!this.authorizationManager.hasAccess(approveRight, groupsApprover,
                        modifiedDocument.getDocumentReference())) {
                        return false;
                    }
                }
            } catch (ChangeRequestException e) {
                this.logger.error("Error while trying to load approvers of modified document from filechange [{}]: "
                    + "[{}]", fileChange, ExceptionUtils.getRootCauseMessage(e));
                this.logger.debug("Full stack trace: ", e);
            }
        }
        return result;
    }
}
