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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ApproversManager;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.internal.UserReferenceConverter;
import org.xwiki.contrib.changerequest.rights.ChangeRequestApproveRight;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.user.UserReferenceSerializer;
import org.xwiki.user.group.GroupException;
import org.xwiki.user.group.GroupManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.xwiki.contrib.changerequest.internal.approvers.ApproversXClassInitializer.APPROVERS_XCLASS;
import static org.xwiki.contrib.changerequest.internal.approvers.ApproversXClassInitializer.GROUPS_APPROVERS_PROPERTY;
import static org.xwiki.contrib.changerequest.internal.approvers.ApproversXClassInitializer.USERS_APPROVERS_PROPERTY;

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
    static final Character SEPARATOR_CHARACTER = ',';

    @Inject
    private UserReferenceResolver<String> stringUserReferenceResolver;

    @Inject
    private GroupManager groupManager;

    @Inject
    @Named("document")
    private UserReferenceResolver<DocumentReference> documentReferenceUserReferenceResolver;

    @Inject
    private UserReferenceSerializer<String> userReferenceSerializer;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private UserReferenceConverter userReferenceConverter;

    @Inject
    private AuthorizationManager authorizationManager;

    private XWikiDocument getDocument(DocumentReference documentReference) throws XWikiException
    {
        XWikiContext context = this.contextProvider.get();
        return context.getWiki().getDocument(documentReference, context);
    }

    private Optional<BaseObject> getApproversObject(DocumentReference entity, boolean create)
        throws ChangeRequestException
    {
        Optional<BaseObject> result = Optional.empty();
        try {
            XWikiDocument document = getDocument(entity);
            BaseObject xObject = document.getXObject(APPROVERS_XCLASS, create, this.contextProvider.get());
            if (xObject != null) {
                result = Optional.of(xObject);
            }
        } catch (XWikiException e) {
            throw new ChangeRequestException(String.format("Error while loading document [%s]", entity), e);
        }
        return result;
    }

    private void writeApproversObject(BaseObject baseObject) throws ChangeRequestException
    {
        try {
            XWikiContext context = this.contextProvider.get();
            context.getWiki().saveDocument(baseObject.getOwnerDocument(), "Save approvers.", true, context);
        } catch (XWikiException e) {
            throw new ChangeRequestException(
                String.format("Error while writing appprovers in document [%s]", baseObject.getOwnerDocument()), e);
        }
    }

    private boolean hasApprovalAccess(UserReference userReference, DocumentReference entity)
    {
        DocumentReference userDocReference = this.userReferenceConverter.convert(userReference);
        Right approveRight = ChangeRequestApproveRight.getRight();
        return this.authorizationManager.hasAccess(approveRight, userDocReference, entity);
    }


    @Override
    public boolean isApprover(UserReference user, DocumentReference entity, boolean explicitOnly)
        throws ChangeRequestException
    {
        Set<UserReference> allApprovers = getAllApprovers(entity, true);
        boolean result = false;
        if (allApprovers.contains(user)) {
            result = true;
        } else if (!explicitOnly && allApprovers.isEmpty()) {
            result = this.hasApprovalAccess(user, entity);
        }
        return result;
    }

    @Override
    public void setUsersApprovers(Set<UserReference> users, DocumentReference entity) throws ChangeRequestException
    {
        Optional<BaseObject> approversObjectOpt = this.getApproversObject(entity, true);
        // we're sure the optional returned something since we used a create boolean value.
        BaseObject baseObject = approversObjectOpt.get();

        List<String> userValues = new ArrayList<>();
        for (UserReference user : users) {
            userValues.add(this.userReferenceSerializer.serialize(user));
        }
        baseObject.setLargeStringValue(USERS_APPROVERS_PROPERTY, StringUtils.join(userValues, SEPARATOR_CHARACTER));
        this.writeApproversObject(baseObject);
    }

    @Override
    public void setGroupsApprovers(Set<DocumentReference> groups, DocumentReference entity)
        throws ChangeRequestException
    {
        Optional<BaseObject> approversObjectOpt = this.getApproversObject(entity, true);
        // we're sure the optional returned something since we used a create boolean value.
        BaseObject baseObject = approversObjectOpt.get();

        List<String> groupValues = new ArrayList<>();
        for (DocumentReference group : groups) {
            groupValues.add(this.entityReferenceSerializer.serialize(group));
        }
        baseObject.setLargeStringValue(GROUPS_APPROVERS_PROPERTY, StringUtils.join(groupValues, SEPARATOR_CHARACTER));
        this.writeApproversObject(baseObject);
    }

    private String[] getValues(BaseObject baseObject, String propertyName)
    {
        String largeStringValue = baseObject.getLargeStringValue(propertyName);
        if (largeStringValue == null) {
            return new String[0];
        } else {
            return StringUtils.split(largeStringValue, SEPARATOR_CHARACTER);
        }
    }

    @Override
    public Set<UserReference> getAllApprovers(DocumentReference entity, boolean recursive) throws ChangeRequestException
    {
        Optional<BaseObject> approversObjectOpt = this.getApproversObject(entity, false);
        Set<UserReference> result = new HashSet<>();
        if (approversObjectOpt.isPresent()) {
            BaseObject baseObject = approversObjectOpt.get();
            String[] stringUsersApprovers = getValues(baseObject, USERS_APPROVERS_PROPERTY);
            for (String stringUsersApprover : stringUsersApprovers) {
                result.add(this.stringUserReferenceResolver.resolve(stringUsersApprover));
            }

            if (recursive) {
                String[] stringGroupsApprovers = getValues(baseObject, GROUPS_APPROVERS_PROPERTY);
                Set<DocumentReference> members = new HashSet<>();
                for (String stringGroupsApprover : stringGroupsApprovers) {
                    DocumentReference groupReference = this.documentReferenceResolver.resolve(stringGroupsApprover);
                    try {
                        members.addAll(this.groupManager.getMembers(groupReference, true));
                    } catch (GroupException e) {
                        throw new ChangeRequestException(
                            String.format("Error when getting members of group [%s].", groupReference), e);
                    }
                }
                for (DocumentReference member : members) {
                    result.add(this.documentReferenceUserReferenceResolver.resolve(member));
                }
            }
        }

        return result;
    }

    @Override
    public Set<DocumentReference> getGroupsApprovers(DocumentReference entity) throws ChangeRequestException
    {
        Optional<BaseObject> approversObjectOpt = this.getApproversObject(entity, false);
        Set<DocumentReference> result = new HashSet<>();
        if (approversObjectOpt.isPresent()) {
            BaseObject baseObject = approversObjectOpt.get();
            String[] stringGroupsApprovers = getValues(baseObject, GROUPS_APPROVERS_PROPERTY);
            for (String stringGroupsApprover : stringGroupsApprovers) {
                result.add(this.documentReferenceResolver.resolve(stringGroupsApprover));
            }
        }
        return result;
    }
}
