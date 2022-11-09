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
package org.xwiki.contrib.changerequest.internal.approvers;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ApproversManager;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.DelegateApproverManager;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.user.UserReference;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * This implementation of {@link ApproversManager} is a read-only implementation dedicated to retrieve the approvers
 * information given a specific filechange. Specifically, if the filechange is of type
 * {@link FileChange.FileChangeType#CREATION} this manager will read in the modified
 * document, the approvers information. Else it will rely on the existing published document. We use that strategy
 * on purpose because we don't want to rely on an edited list of approvers that would override the already existing list
 * of approvers.
 * Note that calling the setters with this implementation will automatically trigger an exception.
 *
 * @version $Id$
 * @since 0.11
 */
@Component
@Singleton
public class FileChangeDelegateApproversManager implements DelegateApproverManager<FileChange>
{
    @Inject
    private DelegateApproverManager<XWikiDocument> documentApproversManager;

    @Inject
    private DelegateApproverManager<DocumentReference> documentReferenceApproversManager;

    @Override
    public Set<UserReference> computeDelegates(UserReference userReference) throws ChangeRequestException
    {
        return this.documentApproversManager.computeDelegates(userReference);
    }

    @Override
    public Set<UserReference> getDelegates(UserReference userReference) throws ChangeRequestException
    {
        return this.documentApproversManager.getDelegates(userReference);
    }

    @Override
    public boolean isDelegateApproverOf(UserReference userReference, FileChange entity)
        throws ChangeRequestException
    {
        if (entity.getType() == FileChange.FileChangeType.CREATION) {
            return this.documentApproversManager
                .isDelegateApproverOf(userReference, (XWikiDocument) entity.getModifiedDocument());
        } else {
            return this.documentReferenceApproversManager.isDelegateApproverOf(userReference, entity.getTargetEntity());
        }
    }

    @Override
    public boolean isDelegateApproverOf(UserReference userReference, FileChange entity, UserReference originalApprover)
        throws ChangeRequestException
    {
        if (entity.getType() == FileChange.FileChangeType.CREATION) {
            return this.documentApproversManager
                .isDelegateApproverOf(userReference, (XWikiDocument) entity.getModifiedDocument(), originalApprover);
        } else {
            return this.documentReferenceApproversManager.isDelegateApproverOf(userReference, entity.getTargetEntity(),
                originalApprover);
        }
    }

    @Override
    public Set<UserReference> getOriginalApprovers(UserReference userReference, FileChange entity)
        throws ChangeRequestException
    {
        if (entity.getType() == FileChange.FileChangeType.CREATION) {
            return this.documentApproversManager
                .getOriginalApprovers(userReference, (XWikiDocument) entity.getModifiedDocument());
        } else {
            return this.documentReferenceApproversManager.getOriginalApprovers(userReference, entity.getTargetEntity());
        }
    }
}
