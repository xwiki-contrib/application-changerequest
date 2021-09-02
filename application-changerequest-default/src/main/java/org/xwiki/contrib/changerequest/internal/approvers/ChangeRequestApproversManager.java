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
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.user.UserReference;

/**
 * Default approvers manager for change request.
 * Since by default change requests are stored as documents, this component is basically a proxy to the
 * {@link DocumentReferenceApproversManager}.
 *
 * @version $Id$
 * @since 0.5
 */
@Component
@Singleton
public class ChangeRequestApproversManager implements ApproversManager<ChangeRequest>
{
    @Inject
    private DocumentReferenceResolver<ChangeRequest> changeRequestDocumentReferenceResolver;

    @Inject
    private ApproversManager<DocumentReference> documentReferenceApproversManager;

    @Override
    public boolean isApprover(UserReference user, ChangeRequest entity, boolean explicitOnly)
        throws ChangeRequestException
    {
        boolean result;
        DocumentReference documentEntity = this.changeRequestDocumentReferenceResolver.resolve(entity);
        // Check order for change request in case of implicit check:
        //  1. explicit approvers list on the change request document
        //  2. approval right on the change request document itself
        //  3. approval right on the documents targeted by the change request only if the approvers list are empty
        boolean isApproverOfCR = this.documentReferenceApproversManager.isApprover(user, documentEntity, explicitOnly);
        if (!explicitOnly && !isApproverOfCR) {
            Set<UserReference> allApprovers = this.getAllApprovers(entity, true);
            if (allApprovers.isEmpty()) {
                Set<DocumentReference> modifiedDocuments = entity.getModifiedDocuments();
                boolean approvalRight = true;
                for (DocumentReference modifiedDocument : modifiedDocuments) {
                    if (!this.documentReferenceApproversManager.isApprover(user, modifiedDocument, false)) {
                        approvalRight = false;
                        break;
                    }
                }
                result = approvalRight;
            } else {
                result = false;
            }
        } else {
            result = isApproverOfCR;
        }
        return result;
    }

    @Override
    public void setUsersApprovers(Set<UserReference> users, ChangeRequest entity)
        throws ChangeRequestException
    {
        DocumentReference documentEntity = this.changeRequestDocumentReferenceResolver.resolve(entity);
        this.documentReferenceApproversManager.setUsersApprovers(users, documentEntity);
    }

    @Override
    public void setGroupsApprovers(Set<DocumentReference> groups, ChangeRequest entity)
        throws ChangeRequestException
    {
        DocumentReference documentEntity = this.changeRequestDocumentReferenceResolver.resolve(entity);
        this.documentReferenceApproversManager.setGroupsApprovers(groups, documentEntity);
    }

    @Override
    public Set<UserReference> getAllApprovers(ChangeRequest entity, boolean recursive)
        throws ChangeRequestException
    {
        DocumentReference documentEntity = this.changeRequestDocumentReferenceResolver.resolve(entity);
        return this.documentReferenceApproversManager.getAllApprovers(documentEntity, recursive);
    }

    @Override
    public Set<DocumentReference> getGroupsApprovers(ChangeRequest entity) throws ChangeRequestException
    {
        DocumentReference documentEntity = this.changeRequestDocumentReferenceResolver.resolve(entity);
        return this.documentReferenceApproversManager.getGroupsApprovers(documentEntity);
    }
}
