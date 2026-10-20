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

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ApproversManager;
import org.xwiki.contrib.changerequest.ChangeRequestConfiguration;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.FileChangeSavingChecker.SavingCheckerResult;
import org.xwiki.contrib.changerequest.internal.UserReferenceConverter;
import org.xwiki.contrib.changerequest.rights.ChangeRequestApproveRight;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.user.UserReference;
import org.xwiki.user.group.GroupException;
import org.xwiki.user.group.GroupManager;

/**
 * Decides whether the approvers provided by the author of a change on an existing document should be used for the
 * change request, and validates them.
 * <p>
 * The approvers of an existing document are normally only defined by the published version of the document: an
 * edited list of approvers is only applied once approved by the existing approvers. This doesn't work when the
 * published document doesn't respect the minimum number of approvers, since nobody legitimate would be there to
 * approve the new list. In such case the list provided by the author is used, provided that the approvers it adds:
 * <ul>
 *     <li>don't contain the author, directly or through a group</li>
 *     <li>have view right on the document</li>
 *     <li>have {@link ChangeRequestApproveRight} on the document</li>
 * </ul>
 * Those conditions don't apply when the author has admin right on the document, since such author is allowed to
 * directly edit the approvers of the document anyway.
 *
 * @version $Id$
 * @since 1.26
 */
@Component(roles = ProvidedApproversValidator.class)
@Singleton
public class ProvidedApproversValidator
{
    private static final String REASON_PREFIX = "changerequest.checkers.providedApprovers.";

    /**
     * Reason used when the author is part of the approvers they added.
     */
    static final String AUTHOR_IS_APPROVER_REASON = REASON_PREFIX + "authorIsApprover";

    /**
     * Reason used when one of the added user approvers doesn't have view right on the document.
     */
    static final String MISSING_VIEW_RIGHT_REASON = REASON_PREFIX + "missingViewRight";

    /**
     * Reason used when one of the added approvers doesn't have approve right on the document.
     */
    static final String MISSING_APPROVE_RIGHT_REASON = REASON_PREFIX + "missingApproveRight";

    @Inject
    private ChangeRequestConfiguration configuration;

    @Inject
    private ApproversManager<DocumentReference> documentReferenceApproversManager;

    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    private GroupManager groupManager;

    @Inject
    private UserReferenceConverter userReferenceConverter;

    @Inject
    private Logger logger;

    /**
     * Check if the approvers provided by the author of a change should be used instead of the ones of the published
     * document.
     *
     * @param documentReference the reference of the existing document being changed
     * @return {@code true} if a minimum of approvers is configured and the published document doesn't respect it
     * @throws ChangeRequestException in case of problem to retrieve the approvers of the published document
     */
    public boolean isProvidedApproversListUsed(DocumentReference documentReference) throws ChangeRequestException
    {
        int minimumApprovers = this.configuration.getMinimumApprovers();
        return minimumApprovers > 0
            && this.documentReferenceApproversManager.getAllApprovers(documentReference, false).size()
            < minimumApprovers;
    }

    /**
     * Validate the approvers provided by the author of a change on an existing document. The validation only applies
     * when {@link #isProvidedApproversListUsed(DocumentReference)} is {@code true} and the author doesn't have admin
     * right on the document, and only to the approvers that are not already approvers of the published document.
     *
     * @param documentReference the reference of the existing document being changed
     * @param author the author of the change
     * @param usersApprovers the user approvers provided by the author
     * @param groupsApprovers the group approvers provided by the author
     * @return a successful result if the provided approvers can be used, else a result holding the translation key
     *     of the reason why they cannot be used
     * @throws ChangeRequestException in case of problem to retrieve the approvers of the published document
     */
    public SavingCheckerResult validate(DocumentReference documentReference, DocumentReference author,
        Collection<DocumentReference> usersApprovers, Collection<DocumentReference> groupsApprovers)
        throws ChangeRequestException
    {
        SavingCheckerResult result = new SavingCheckerResult();
        if (this.isProvidedApproversListUsed(documentReference)
            && !this.authorizationManager.hasAccess(Right.ADMIN, author, documentReference)) {
            Set<DocumentReference> addedUsers = new LinkedHashSet<>(usersApprovers);
            for (UserReference existingApprover
                : this.documentReferenceApproversManager.getAllApprovers(documentReference, false)) {
                addedUsers.remove(this.userReferenceConverter.convert(existingApprover));
            }
            Set<DocumentReference> addedGroups = new LinkedHashSet<>(groupsApprovers);
            addedGroups.removeAll(this.documentReferenceApproversManager.getGroupsApprovers(documentReference));

            if (this.isAuthorContained(author, addedUsers, addedGroups)) {
                result = new SavingCheckerResult(AUTHOR_IS_APPROVER_REASON);
            } else if (!this.haveAccess(Right.VIEW, addedUsers, documentReference)) {
                result = new SavingCheckerResult(MISSING_VIEW_RIGHT_REASON);
            } else if (!this.haveAccess(ChangeRequestApproveRight.getRight(), addedUsers, documentReference)
                || !this.haveAccess(ChangeRequestApproveRight.getRight(), addedGroups, documentReference)) {
                result = new SavingCheckerResult(MISSING_APPROVE_RIGHT_REASON);
            }
        }
        return result;
    }

    private boolean isAuthorContained(DocumentReference author, Set<DocumentReference> addedUsers,
        Set<DocumentReference> addedGroups)
    {
        boolean result = addedUsers.contains(author);
        if (!result) {
            for (DocumentReference addedGroup : addedGroups) {
                try {
                    if (this.groupManager.getMembers(addedGroup, true).contains(author)) {
                        result = true;
                        break;
                    }
                } catch (GroupException e) {
                    this.logger.warn("Error while trying to get members of group [{}]: [{}]", addedGroup,
                        ExceptionUtils.getRootCauseMessage(e));
                }
            }
        }
        return result;
    }

    private boolean haveAccess(Right right, Set<DocumentReference> entities, DocumentReference documentReference)
    {
        boolean result = true;
        for (DocumentReference entity : entities) {
            if (!this.authorizationManager.hasAccess(right, entity, documentReference)) {
                result = false;
                break;
            }
        }
        return result;
    }
}
