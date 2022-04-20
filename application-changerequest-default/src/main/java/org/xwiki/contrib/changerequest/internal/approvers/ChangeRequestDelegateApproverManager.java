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
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.DelegateApproverManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.user.UserReference;

@Component
@Singleton
public class ChangeRequestDelegateApproverManager implements DelegateApproverManager<ChangeRequest>
{
    @Inject
    private DelegateApproverManager<DocumentReference> documentReferenceDelegateApproverManager;

    @Inject
    private DocumentReferenceResolver<ChangeRequest> changeRequestDocumentReferenceResolver;

    @Override
    public Set<UserReference> computeDelegates(UserReference userReference) throws ChangeRequestException
    {
        return this.documentReferenceDelegateApproverManager.computeDelegates(userReference);
    }

    @Override
    public Set<UserReference> getDelegates(UserReference userReference)
    {
        return this.documentReferenceDelegateApproverManager.getDelegates(userReference);
    }

    @Override
    public boolean isDelegateApproverOf(UserReference userReference, ChangeRequest entity)
        throws ChangeRequestException
    {
        DocumentReference documentReference = this.changeRequestDocumentReferenceResolver.resolve(entity);
        return this.documentReferenceDelegateApproverManager.isDelegateApproverOf(userReference, documentReference);
    }

    @Override
    public boolean isDelegateApproverOf(UserReference userReference, ChangeRequest entity,
        UserReference originalApprover) throws ChangeRequestException
    {
        DocumentReference documentReference = this.changeRequestDocumentReferenceResolver.resolve(entity);
        return this.documentReferenceDelegateApproverManager
            .isDelegateApproverOf(userReference, documentReference, originalApprover);
    }

    @Override
    public Set<UserReference> getOriginalApprovers(UserReference userReference, ChangeRequest entity)
        throws ChangeRequestException
    {
        DocumentReference documentReference = this.changeRequestDocumentReferenceResolver.resolve(entity);
        return this.documentReferenceDelegateApproverManager.getOriginalApprovers(userReference, documentReference);
    }
}
