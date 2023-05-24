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
import java.util.LinkedHashSet;
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
import org.xwiki.contrib.changerequest.DelegateApproverManager;
import org.xwiki.contrib.changerequest.internal.UserReferenceConverter;
import org.xwiki.contrib.changerequest.rights.ChangeRequestApproveRight;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.user.CurrentUserReference;
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
 * Default implementation of {@link ApproversManager} that uses direct {@link XWikiDocument}.
 * Note that most of the time, the {@link DocumentReferenceApproversManager} should be preferred: this implementation
 * is provided mainly to be used with {@link FileChangeApproversManager}.
 *
 * @version $Id$
 * @since 0.11
 */
@Component
@Singleton
public class XWikiDocumentApproversManager implements ApproversManager<XWikiDocument>
{
    @Inject
    @Named("current")
    private UserReferenceResolver<String> stringUserReferenceResolver;

    @Inject
    private GroupManager groupManager;

    @Inject
    @Named("document")
    private UserReferenceResolver<DocumentReference> documentReferenceUserReferenceResolver;

    @Inject
    private UserReferenceResolver<CurrentUserReference> currentUserReferenceUserReferenceResolver;

    @Inject
    private UserReferenceSerializer<String> userReferenceSerializer;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private UserReferenceConverter userReferenceConverter;

    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    private DelegateApproverManager<XWikiDocument> documentDelegateApproverManager;

    private Optional<BaseObject> getApproversObject(XWikiDocument document, boolean create)
        throws ChangeRequestException
    {
        Optional<BaseObject> result = Optional.empty();
        BaseObject xObject = document.getXObject(APPROVERS_XCLASS, create, this.contextProvider.get());
        if (xObject != null) {
            result = Optional.of(xObject);
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

    private boolean hasApprovalAccess(UserReference userReference, XWikiDocument entity)
    {
        DocumentReference userDocReference = this.userReferenceConverter.convert(userReference);
        Right approveRight = ChangeRequestApproveRight.getRight();
        return this.authorizationManager.hasAccess(Right.ADMIN, userDocReference, entity.getDocumentReference())
            || this.authorizationManager.hasAccess(approveRight, userDocReference, entity.getDocumentReference());
    }

    @Override
    public boolean isApprover(UserReference user, XWikiDocument entity, boolean explicitOnly)
        throws ChangeRequestException
    {
        Set<UserReference> allApprovers = getAllApprovers(entity, true);
        boolean result = false;
        UserReference userReference;
        if (user == CurrentUserReference.INSTANCE) {
            userReference = this.currentUserReferenceUserReferenceResolver.resolve(CurrentUserReference.INSTANCE);
        } else {
            userReference = user;
        }
        if (allApprovers.contains(userReference)) {
            result = true;
        } else if (!explicitOnly && allApprovers.isEmpty()) {
            result = this.hasApprovalAccess(userReference, entity);
        }
        return result;
    }

    @Override
    public void setUsersApprovers(Set<UserReference> users, XWikiDocument entity)
        throws ChangeRequestException
    {
        Optional<BaseObject> approversObjectOpt = this.getApproversObject(entity, true);
        // we're sure the optional returned something since we used a create boolean value.
        BaseObject baseObject = approversObjectOpt.get();

        List<String> userValues = new ArrayList<>();
        for (UserReference user : users) {
            userValues.add(this.userReferenceSerializer.serialize(user));
        }
        baseObject.setLargeStringValue(USERS_APPROVERS_PROPERTY, StringUtils.join(userValues,
            ApproversXClassInitializer.SEPARATOR_CHARACTER));
        this.writeApproversObject(baseObject);
    }

    @Override
    public void setGroupsApprovers(Set<DocumentReference> groups, XWikiDocument entity)
        throws ChangeRequestException
    {
        Optional<BaseObject> approversObjectOpt = this.getApproversObject(entity, true);
        // we're sure the optional returned something since we used a create boolean value.
        BaseObject baseObject = approversObjectOpt.get();

        List<String> groupValues = new ArrayList<>();
        for (DocumentReference group : groups) {
            groupValues.add(this.entityReferenceSerializer.serialize(group));
        }
        baseObject.setLargeStringValue(GROUPS_APPROVERS_PROPERTY, StringUtils.join(groupValues,
            ApproversXClassInitializer.SEPARATOR_CHARACTER));
        this.writeApproversObject(baseObject);
    }

    private String[] getValues(BaseObject baseObject, String propertyName)
    {
        String largeStringValue = baseObject.getLargeStringValue(propertyName);
        if (largeStringValue == null) {
            return new String[0];
        } else {
            return StringUtils.split(largeStringValue, ApproversXClassInitializer.SEPARATOR_CHARACTER);
        }
    }

    @Override
    public Set<UserReference> getAllApprovers(XWikiDocument entity, boolean recursive)
        throws ChangeRequestException
    {
        Optional<BaseObject> approversObjectOpt = this.getApproversObject(entity, false);
        Set<UserReference> result = new LinkedHashSet<>();
        if (approversObjectOpt.isPresent()) {
            BaseObject baseObject = approversObjectOpt.get();
            String[] stringUsersApprovers = getValues(baseObject, USERS_APPROVERS_PROPERTY);
            for (String stringUsersApprover : stringUsersApprovers) {
                result.add(this.stringUserReferenceResolver.resolve(stringUsersApprover));
            }

            if (recursive) {
                String[] stringGroupsApprovers = getValues(baseObject, GROUPS_APPROVERS_PROPERTY);
                Set<DocumentReference> members = new LinkedHashSet<>();
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
    public Set<DocumentReference> getGroupsApprovers(XWikiDocument entity) throws ChangeRequestException
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

    @Override
    public boolean wasManuallyEdited(XWikiDocument entity) throws ChangeRequestException
    {
        Optional<BaseObject> approversObjectOpt = this.getApproversObject(entity, false);
        boolean result = false;
        if (approversObjectOpt.isPresent()) {
            BaseObject baseObject = approversObjectOpt.get();
            result = baseObject.getIntValue(ApproversXClassInitializer.MANUAL_EDITION_PROPERTY) == 1;
        }
        return result;
    }
}
