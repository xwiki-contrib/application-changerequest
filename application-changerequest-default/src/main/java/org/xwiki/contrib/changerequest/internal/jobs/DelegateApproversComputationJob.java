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
package org.xwiki.contrib.changerequest.internal.jobs;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.DelegateApproverManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.refactoring.internal.job.AbstractEntityJob;
import org.xwiki.refactoring.job.EntityJobStatus;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

/**
 * Job implementation for the computation of delegate approvers based on XWikiUsers fields.
 * This job is only a proxy to {@link DelegateApproverManager#computeDelegates(UserReference)}.
 *
 * @version $Id$
 * @since 0.13
 */
@Component
@Named(DelegateApproversComputationRequest.DELEGATE_APPROVERS_COMPUTATION_JOB)
public class DelegateApproversComputationJob extends
    AbstractEntityJob<DelegateApproversComputationRequest, EntityJobStatus<DelegateApproversComputationRequest>>
{
    @Inject
    @Named("document")
    private UserReferenceResolver<DocumentReference> documentReferenceUserReferenceResolver;

    @Inject
    private DelegateApproverManager<DocumentReference> delegateApproverManager;

    @Override
    protected void process(EntityReference entityReference)
    {
        if (entityReference instanceof DocumentReference) {
            UserReference userReference =
                this.documentReferenceUserReferenceResolver.resolve((DocumentReference) entityReference);
            try {
                this.delegateApproverManager.computeDelegates(userReference);
            } catch (ChangeRequestException e) {
                this.logger.error("Error while computing delegate for [{}]", userReference, e);
            }
        }
    }

    @Override
    public String getType()
    {
        return DelegateApproversComputationRequest.DELEGATE_APPROVERS_COMPUTATION_JOB;
    }
}
