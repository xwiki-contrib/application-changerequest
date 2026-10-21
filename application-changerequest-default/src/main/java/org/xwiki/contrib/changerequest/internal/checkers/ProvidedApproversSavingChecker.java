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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ApproversManager;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.FileChangeSavingChecker;
import org.xwiki.contrib.changerequest.internal.UserReferenceConverter;
import org.xwiki.contrib.changerequest.internal.approvers.ProvidedApproversValidator;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.user.UserReference;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Checker ensuring that the approvers provided by the author of a change on a document which doesn't respect the
 * minimum number of approvers can be used for the change request.
 *
 * @version $Id$
 * @since 1.26
 * @see ProvidedApproversValidator
 */
@Component
@Named("ProvidedApproversSavingChecker")
@Singleton
@Priority(130)
public class ProvidedApproversSavingChecker implements FileChangeSavingChecker
{
    private static final String ERROR_REASON = "changerequest.checkers.providedApprovers.error";

    @Inject
    private ProvidedApproversValidator providedApproversValidator;

    @Inject
    private ApproversManager<XWikiDocument> documentApproversManager;

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
        return this.check(fileChange);
    }

    @Override
    public SavingCheckerResult canChangeRequestBeCreatedWith(FileChange fileChange)
    {
        return this.check(fileChange);
    }

    private SavingCheckerResult check(FileChange fileChange)
    {
        SavingCheckerResult result = new SavingCheckerResult();
        if (fileChange.getType() == FileChange.FileChangeType.EDITION && fileChange.getModifiedDocument() != null) {
            XWikiDocument modifiedDocument = (XWikiDocument) fileChange.getModifiedDocument();
            try {
                // The provided approvers only need to be checked when they are used, which only happens when the
                // targeted document doesn't respect the minimum number of approvers.
                if (this.providedApproversValidator.isProvidedApproversListUsed(fileChange.getTargetEntity())) {
                    List<DocumentReference> usersApprovers = new ArrayList<>();
                    for (UserReference userApprover : this.documentApproversManager.getAllApprovers(modifiedDocument,
                        false)) {
                        usersApprovers.add(this.userReferenceConverter.convert(userApprover));
                    }
                    result = this.providedApproversValidator.validate(fileChange.getTargetEntity(),
                        this.userReferenceConverter.convert(fileChange.getAuthor()), usersApprovers,
                        this.documentApproversManager.getGroupsApprovers(modifiedDocument));
                }
            } catch (ChangeRequestException e) {
                this.logger.error("Error while checking the approvers provided in filechange [{}]: [{}]", fileChange,
                    ExceptionUtils.getRootCauseMessage(e));
                this.logger.debug("Full stack trace: ", e);
                result = new SavingCheckerResult(ERROR_REASON);
            }
        }
        return result;
    }
}
