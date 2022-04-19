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
import org.xwiki.contrib.changerequest.ApproversManager;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.user.UserReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Default approvers manager for standard documents.
 * We are relying on an xobject inserted in documents to define the list of approvers.
 *
 * @version $Id$
 * @since 0.5
 */
@Component
@Singleton
public class DocumentReferenceApproversManager implements ApproversManager<DocumentReference>
{
    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private ApproversManager<XWikiDocument> documentApproversManager;

    private XWikiDocument getDocument(DocumentReference documentReference) throws ChangeRequestException
    {
        XWikiContext context = this.contextProvider.get();
        try {
            return context.getWiki().getDocument(documentReference, context);
        } catch (XWikiException e) {
            throw new ChangeRequestException(String.format("Error while loading document [%s]", documentReference), e);
        }
    }

    @Override
    public boolean isApprover(UserReference user, DocumentReference entity, boolean explicitOnly)
        throws ChangeRequestException
    {
        return this.documentApproversManager.isApprover(user, getDocument(entity), explicitOnly);
    }

    @Override
    public void setUsersApprovers(Set<UserReference> users, DocumentReference entity)
        throws ChangeRequestException
    {
        this.documentApproversManager.setUsersApprovers(users, getDocument(entity));
    }

    @Override
    public void setGroupsApprovers(Set<DocumentReference> groups, DocumentReference entity)
        throws ChangeRequestException
    {
        this.documentApproversManager.setGroupsApprovers(groups, getDocument(entity));
    }

    @Override
    public Set<UserReference> getAllApprovers(DocumentReference entity, boolean recursive)
        throws ChangeRequestException
    {
        return this.documentApproversManager.getAllApprovers(getDocument(entity), recursive);
    }

    @Override
    public Set<DocumentReference> getGroupsApprovers(DocumentReference entity) throws ChangeRequestException
    {
        return this.documentApproversManager.getGroupsApprovers(getDocument(entity));
    }

    @Override
    public boolean wasManuallyEdited(DocumentReference entity) throws ChangeRequestException
    {
        return this.documentApproversManager.wasManuallyEdited(getDocument(entity));
    }
}
