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

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestStatus;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.FileChangeSavingChecker;
import org.xwiki.model.reference.DocumentReference;

/**
 * Checker that only ensure the change request is not merged or cancelled.
 *
 * @version $Id$
 * @since 0.9
 */
@Component
@Named("org.xwiki.contrib.changerequest.internal.checkers.ChangeRequestStatusChecker")
@Singleton
public class ChangeRequestStatusChecker implements FileChangeSavingChecker
{
    @Override
    public SavingCheckerResult canChangeOnDocumentBeAdded(ChangeRequest changeRequest,
        DocumentReference documentReference, FileChange.FileChangeType fileChangeType)
    {
        ChangeRequestStatus status = changeRequest.getStatus();
        if (status != ChangeRequestStatus.CLOSED && status != ChangeRequestStatus.MERGED) {
            return new SavingCheckerResult();
        } else {
            return new SavingCheckerResult("changerequest.checkers.status.incompatibilityReason");
        }
    }

    @Override
    public SavingCheckerResult canChangeRequestBeCreatedWith(FileChange fileChange)
    {
        return new SavingCheckerResult();
    }
}
