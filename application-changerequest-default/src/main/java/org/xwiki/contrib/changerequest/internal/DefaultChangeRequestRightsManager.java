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
package org.xwiki.contrib.changerequest.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestRightsManager;
import org.xwiki.contrib.rights.RightsReader;
import org.xwiki.contrib.rights.RightsWriter;
import org.xwiki.contrib.rights.SecurityRuleAbacus;
import org.xwiki.contrib.rights.WritableSecurityRule;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.security.authorization.AuthorizationException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.ReadableSecurityRule;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.RightSet;

import com.xpn.xwiki.XWikiException;

/**
 * Component in charge of performing right synchronization operations.
 *
 * @version $Id$
 * @since 0.7
 */
@Component
@Singleton
public class DefaultChangeRequestRightsManager implements ChangeRequestRightsManager
{
    @Inject
    private DocumentReferenceResolver<ChangeRequest> changeRequestDocumentReferenceResolver;

    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    private RightsWriter rightsWriter;

    @Inject
    private SecurityRuleAbacus ruleAbacus;

    @Inject
    private RightsReader rightsReader;

    @Override
    public void copyAllButViewRights(ChangeRequest originalChangeRequest, ChangeRequest targetChangeRequest)
        throws ChangeRequestException
    {
        DocumentReference originalDocReference =
            this.changeRequestDocumentReferenceResolver.resolve(originalChangeRequest);
        DocumentReference targetDocReference =
            this.changeRequestDocumentReferenceResolver.resolve(targetChangeRequest);

        try {
            List<ReadableSecurityRule> actualRules =
                this.rightsReader.getActualRules(originalDocReference.getLastSpaceReference(), false);
            List<ReadableSecurityRule> writableSecurityRules = new ArrayList<>();

            for (ReadableSecurityRule actualRule : actualRules) {
                if (actualRule.match(Right.VIEW)) {
                    WritableSecurityRule rule = this.rightsWriter.createRule(actualRule);
                    RightSet rights = rule.getRights();
                    rights.remove(Right.VIEW);
                    if (!rights.isEmpty()) {
                        rule.setRights(rights);
                        writableSecurityRules.add(rule);
                    }
                } else {
                    writableSecurityRules.add(actualRule);
                }
            }
            this.rightsWriter.saveRules(writableSecurityRules, targetDocReference.getLastSpaceReference());
        } catch (AuthorizationException | XWikiException e) {
            throw new ChangeRequestException(
                String.format("Error while trying to retrieve or save rights between [%s] and [%s]",
                    originalDocReference, targetDocReference), e);
        }
    }

    @Override
    public boolean isViewAccessConsistent(ChangeRequest changeRequest, DocumentReference newChange)
        throws ChangeRequestException
    {
        Set<DocumentReference> documentReferences = new HashSet<>(changeRequest.getModifiedDocuments());
        documentReferences.add(newChange);

        Set<DocumentReference> subjects = new HashSet<>();
        for (DocumentReference documentReference : documentReferences) {
            try {
                List<ReadableSecurityRule> actualRules = this.rightsReader.getActualRules(documentReference);
                List<ReadableSecurityRule> normalizedRules = this.ruleAbacus.normalizeRulesBySubject(actualRules);
                for (ReadableSecurityRule normalizedRule : normalizedRules) {
                    if (normalizedRule.match(Right.VIEW)) {
                        subjects.addAll(normalizedRule.getGroups());
                        subjects.addAll(normalizedRule.getUsers());
                    }
                }
            } catch (AuthorizationException e) {
                throw new ChangeRequestException(
                    String.format("Error while trying to access rights for [%s]", documentReference), e);
            }
        }

        return this.isViewAccessConsistent(documentReferences, subjects);
    }

    @Override
    public boolean isViewAccessStillConsistent(ChangeRequest changeRequest,
        Set<DocumentReference> subjectReferences) throws ChangeRequestException
    {
        return this.isViewAccessConsistent(changeRequest.getModifiedDocuments(), subjectReferences);
    }

    private boolean isViewAccessConsistent(Set<DocumentReference> documentReferences,
        Set<DocumentReference> subjectReferences)
    {
        for (DocumentReference subject : subjectReferences) {
            Boolean hasAccess = null;
            for (DocumentReference modifiedDocument : documentReferences) {
                boolean currentHasAccess =
                    this.authorizationManager.hasAccess(Right.VIEW, subject, modifiedDocument);

                if (hasAccess == null) {
                    hasAccess = currentHasAccess;
                } else if (hasAccess != currentHasAccess) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void copyViewRights(ChangeRequest changeRequest, EntityReference newChange)
        throws ChangeRequestException
    {
        DocumentReference changeRequestDocReference =
            this.changeRequestDocumentReferenceResolver.resolve(changeRequest);
        SpaceReference changeRequestSpaceReference = changeRequestDocReference.getLastSpaceReference();

        try {
            List<ReadableSecurityRule> actualRules =
                this.rightsReader.getActualRules(changeRequestSpaceReference, false);
            List<ReadableSecurityRule> rules = new ArrayList<>(this.rightsWriter.createRules(actualRules));
            List<ReadableSecurityRule> documentRules = new ArrayList<>(this.rightsReader.getActualRules(newChange));
            List<ReadableSecurityRule> wikiRules =
                this.rightsReader.getActualRules(newChange.extractReference(EntityType.WIKI));

            // we filter out the wiki reference rules
            documentRules.removeAll(wikiRules);
            for (ReadableSecurityRule actualRule : documentRules) {
                if (actualRule.match(Right.VIEW)) {
                    WritableSecurityRule rule = this.rightsWriter.createRule(actualRule);
                    rule.setRights(Collections.singletonList(Right.VIEW));
                    rules.add(rule);
                }
            }

            this.rightsWriter.saveRules(rules, changeRequestSpaceReference);
        } catch (AuthorizationException | XWikiException e) {
            throw new ChangeRequestException(
                String.format("Error while copying rights from [%s] for change request [%s]", changeRequest, newChange),
                e);
        }
    }
}
