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
package org.xwiki.contrib.changerequest.internal.listeners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.events.ChangeRequestMergingEvent;
import org.xwiki.contrib.changerequest.events.ChangeRequestUpdatingFileChangeEvent;
import org.xwiki.contrib.changerequest.internal.approvers.ApproversXClassInitializer;
import org.xwiki.contrib.changerequest.internal.approvers.DocumentReferenceApproversManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.ObservationContext;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.RemoteObservationManagerContext;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.user.group.GroupException;
import org.xwiki.user.group.GroupManager;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.UserEvent;
import com.xpn.xwiki.internal.event.UserUpdatingDocumentEvent;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Listener in charge of checking the updates performed on approvers list to discard changes done on behalf of the
 * current user: basically the current user should not be able to add themselves from the list of approvers.
 *
 * @version $Id$
 * @since 0.8
 */
@Component
@Named(ApproversXObjectUpdatingListener.NAME)
@Singleton
public class ApproversXObjectUpdatingListener extends AbstractEventListener
{
    /**
     * The name of the listener.
     */
    static final String NAME = "org.xwiki.contrib.changerequest.internal.listeners.ApproversXObjectUpdatingListener";

    // We rely on user updating document event to be sure to rely on the proper user:
    // we don't want that listener to interfere in case of saving a document by merging a change request.
    private static final List<Event> EVENT_LIST = Collections.singletonList(
        new UserUpdatingDocumentEvent()
    );

    @Inject
    private GroupManager groupManager;

    @Inject
    private DocumentReferenceResolver<String> stringDocumentReferenceResolver;

    @Inject
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @Inject
    private ObservationContext observationContext;

    @Inject
    private RemoteObservationManagerContext remoteObservationManagerContext;

    @Inject
    private Logger logger;

    /**
     * Default constructor.
     */
    public ApproversXObjectUpdatingListener()
    {
        super(NAME, EVENT_LIST);
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        // We don't perform any check if current user has admin right
        // we want admin to be able to perform this kind of change.
        // We also don't perform any check in case of merging a change request: in such case the list of approvers
        // has been approved during CR review.
        // Finally we don't take into account remote events.
        if (!this.contextualAuthorizationManager.hasAccess(Right.ADMIN)
            && !this.observationContext.isIn(new ChangeRequestMergingEvent())
            && !this.observationContext.isIn(new ChangeRequestUpdatingFileChangeEvent())
            && !this.remoteObservationManagerContext.isRemoteState()) {
            XWikiDocument document = (XWikiDocument) source;

            UserEvent userEvent = (UserEvent) event;
            XWikiDocument originalDocument = document.getOriginalDocument();
            List<BaseObject> originalApprovers =
                originalDocument.getXObjects(ApproversXClassInitializer.APPROVERS_XCLASS);
            List<BaseObject> approvers = document.getXObjects(ApproversXClassInitializer.APPROVERS_XCLASS);

            if (this.shouldApproversChangedBeCancelled(userEvent.getUserReference(), originalApprovers, approvers)) {
                this.cancel(document, originalApprovers, approvers);
            }
        }
    }

    private List<DocumentReference> resolveLists(List<String> list)
    {
        List<DocumentReference> result = new ArrayList<>();
        for (String serializedRef : list) {
            result.add(this.stringDocumentReferenceResolver.resolve(serializedRef));
        }
        return result;
    }

    private String[] getValues(BaseObject baseObject, String propertyName)
    {
        String largeStringValue = baseObject.getLargeStringValue(propertyName);
        if (largeStringValue == null) {
            return new String[0];
        } else {
            return StringUtils.split(largeStringValue, DocumentReferenceApproversManager.SEPARATOR_CHARACTER);
        }
    }

    private List<DocumentReference> getAddedGroups(BaseObject previousObject, BaseObject nextObject)
    {
        List<String> previousGroups = new ArrayList<>();
        if (previousObject != null) {
            previousGroups =
                Arrays.asList(getValues(previousObject, ApproversXClassInitializer.GROUPS_APPROVERS_PROPERTY));
        }

        List<String> nextGroups =
            Arrays.asList(getValues(nextObject, ApproversXClassInitializer.GROUPS_APPROVERS_PROPERTY));

        List<String> newGroupsString = new ArrayList<>(nextGroups);
        newGroupsString.removeAll(previousGroups);

        return this.resolveLists(newGroupsString);
    }

    private List<DocumentReference> getAddedUsers(BaseObject previousObject, BaseObject nextObject)
    {
        List<String> previousUsers = new ArrayList<>();
        if (previousObject != null) {
            previousUsers =
                Arrays.asList(getValues(previousObject, ApproversXClassInitializer.USERS_APPROVERS_PROPERTY));
        }

        List<String> nextUsers =
            Arrays.asList(getValues(nextObject, ApproversXClassInitializer.USERS_APPROVERS_PROPERTY));

        List<String> newUsersString = new ArrayList<>(nextUsers);
        newUsersString.removeAll(previousUsers);

        return this.resolveLists(newUsersString);
    }

    private boolean shouldSaveBeReverted(DocumentReference currentUser,
        List<DocumentReference> newUsers, List<DocumentReference> newGroups)
    {
        boolean result = false;
        if (newUsers.contains(currentUser)) {
            result = true;
        } else {
            for (DocumentReference newGroup : newGroups) {
                try {
                    Collection<DocumentReference> members = this.groupManager.getMembers(newGroup, true);
                    if (members.contains(currentUser)) {
                        result = true;
                        break;
                    }
                } catch (GroupException e) {
                    logger.warn("Error while trying to get members of group [{}]: [{}]", newGroup,
                        ExceptionUtils.getRootCauseMessage(e));
                }
            }
        }
        return result;
    }

    private boolean shouldCancelApproverUpdate(DocumentReference user, BaseObject originalApprover,
        BaseObject newApprover)
    {
        boolean shouldCancel = false;
        if (newApprover != null) {
            List<DocumentReference> addedGroups = this.getAddedGroups(originalApprover, newApprover);
            List<DocumentReference> addedUsers = this.getAddedUsers(originalApprover, newApprover);
            shouldCancel = this.shouldSaveBeReverted(user, addedUsers, addedGroups);
        }
        return shouldCancel;
    }

    // Directly inspired from code in RightsFilterListener
    private boolean shouldApproversChangedBeCancelled(DocumentReference user, List<BaseObject> originalApproversObjects,
        List<BaseObject> approversObjects)
    {
        boolean shouldCancel = false;
        for (int i = 0; i < originalApproversObjects.size() || i < approversObjects.size(); ++i) {
            BaseObject originalApproversObject =
                (i < originalApproversObjects.size()) ? originalApproversObjects.get(i) : null;
            BaseObject approversObject = (i < approversObjects.size()) ? approversObjects.get(i) : null;

            if (!Objects.equals(originalApproversObject, approversObject)) {
                if (this.shouldCancelApproverUpdate(user, originalApproversObject, approversObject)) {
                    shouldCancel = true;
                    break;
                }
            }
        }
        return shouldCancel;
    }

    // Directly inspired from code in RightsFilterListener
    private void cancel(XWikiDocument document, List<BaseObject> originalApprovers, List<BaseObject> newApprovers)
    {
        for (int i = 0; i < originalApprovers.size() || i < newApprovers.size(); ++i) {
            BaseObject originalApproverObject = (i < originalApprovers.size()) ? originalApprovers.get(i) : null;
            BaseObject approverObject = (i < newApprovers.size()) ? newApprovers.get(i) : null;

            if (originalApproverObject != null) {
                if (approverObject != null) {
                    approverObject.apply(originalApproverObject, true);
                } else {
                    document.setXObject(originalApproverObject.getNumber(), originalApproverObject.clone());
                }
            } else if (approverObject != null) {
                document.removeXObject(approverObject);
            }
        }
    }
}
