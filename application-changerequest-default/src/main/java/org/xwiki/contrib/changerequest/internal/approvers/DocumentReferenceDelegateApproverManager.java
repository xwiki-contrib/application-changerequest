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
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.DelegateApproverManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.user.UserReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Default implementation of {@link DelegateApproverManager} for {@link DocumentReference} entity.
 *
 * @version $Id$
 * @since 0.13
 */
@Component
@Singleton
public class DocumentReferenceDelegateApproverManager implements DelegateApproverManager<DocumentReference>
{
    private static final String ERROR_MSG = "Error when reading the document [%s] to get approvers";

    @Inject
    private DelegateApproverManager<XWikiDocument> documentDelegateApproverManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Override
    public Set<UserReference> computeDelegates(UserReference userReference) throws ChangeRequestException
    {
        return this.documentDelegateApproverManager.computeDelegates(userReference);
    }

    @Override
    public Set<UserReference> getDelegates(UserReference userReference) throws ChangeRequestException
    {
        return this.documentDelegateApproverManager.getDelegates(userReference);
    }

    @Override
    public boolean isDelegateApproverOf(UserReference userReference, DocumentReference entity)
        throws ChangeRequestException
    {
        XWikiContext context = this.contextProvider.get();
        try {
            XWikiDocument document = context.getWiki().getDocument(entity, context);
            return this.documentDelegateApproverManager.isDelegateApproverOf(userReference, document);
        } catch (XWikiException e) {
            throw new ChangeRequestException(
                String.format(ERROR_MSG, entity), e);
        }
    }

    @Override
    public boolean isDelegateApproverOf(UserReference userReference, DocumentReference entity,
        UserReference originalApprover) throws ChangeRequestException
    {
        XWikiContext context = this.contextProvider.get();
        try {
            XWikiDocument document = context.getWiki().getDocument(entity, context);
            return this.documentDelegateApproverManager.isDelegateApproverOf(userReference, document, originalApprover);
        } catch (XWikiException e) {
            throw new ChangeRequestException(
                String.format(ERROR_MSG, entity), e);
        }
    }

    @Override
    public Set<UserReference> getOriginalApprovers(UserReference userReference, DocumentReference entity)
        throws ChangeRequestException
    {
        XWikiContext context = this.contextProvider.get();
        Set<UserReference> result;
        try {
            XWikiDocument document = context.getWiki().getDocument(entity, context);
            result = this.documentDelegateApproverManager.getOriginalApprovers(userReference, document);
        } catch (XWikiException e) {
            throw new ChangeRequestException(
                String.format(ERROR_MSG, entity), e);
        }
        return result;
    }
}
