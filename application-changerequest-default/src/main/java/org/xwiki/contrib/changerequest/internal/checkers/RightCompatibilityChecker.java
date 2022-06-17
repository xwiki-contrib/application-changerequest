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
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestRightsManager;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.FileChangeCompatibilityChecker;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;

/**
 * Default compatibility checker that only checks if the right will be still consistent after adding a file.
 *
 * @version $Id$
 * @since 0.9
 */
@Component
@Named("org.xwiki.contrib.changerequest.internal.checkers.RightCompatibilityChecker")
@Singleton
public class RightCompatibilityChecker implements FileChangeCompatibilityChecker
{
    @Inject
    private ChangeRequestRightsManager changeRequestRightsManager;

    @Inject
    private ContextualLocalizationManager contextualLocalizationManager;

    @Inject
    private Logger logger;

    @Override
    public boolean canChangeOnDocumentBeAdded(ChangeRequest changeRequest, DocumentReference documentReference,
        FileChange.FileChangeType changeType)
    {
        try {
            return this.changeRequestRightsManager.isViewAccessConsistent(changeRequest, documentReference);
        } catch (ChangeRequestException e) {
            logger.warn("Error while checking right consistency of change request [{}] with adding [{}]: [{}]",
                changeRequest.getId(), documentReference, ExceptionUtils.getRootCauseMessage(e));
            return false;
        }
    }

    @Override
    public String getIncompatibilityReason(ChangeRequest changeRequest, DocumentReference documentReference,
        FileChange.FileChangeType changeType)
    {
        return this.contextualLocalizationManager
            .getTranslationPlain("changerequest.checkers.right.incompatibilityReason");
    }
}
