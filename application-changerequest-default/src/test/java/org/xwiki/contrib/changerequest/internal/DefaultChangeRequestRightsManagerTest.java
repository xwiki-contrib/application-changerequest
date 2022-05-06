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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.xwiki.contrib.changerequest.ApproversManager;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestConfiguration;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestStatus;
import org.xwiki.contrib.changerequest.DelegateApproverManager;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.internal.approvers.ChangeRequestApproversManager;
import org.xwiki.contrib.rights.RightsReader;
import org.xwiki.contrib.rights.RightsWriter;
import org.xwiki.contrib.rights.SecurityRuleAbacus;
import org.xwiki.contrib.rights.SecurityRuleDiff;
import org.xwiki.contrib.rights.WritableSecurityRule;
import org.xwiki.contrib.rights.internal.WritableSecurityRuleImpl;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.authorization.AuthorizationException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.ReadableSecurityRule;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.RightSet;
import org.xwiki.security.authorization.RuleState;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.GuestUserReference;
import org.xwiki.user.UserReference;

import com.xpn.xwiki.XWikiException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultChangeRequestRightsManager}.
 *
 * @version $Id$
 * @since 0.7
 */
@ComponentTest
class DefaultChangeRequestRightsManagerTest
{
    @InjectMockComponents
    private DefaultChangeRequestRightsManager rightsManager;

    @MockComponent
    private DocumentReferenceResolver<ChangeRequest> changeRequestDocumentReferenceResolver;

    @MockComponent
    private AuthorizationManager authorizationManager;

    @MockComponent
    private RightsWriter rightsWriter;

    @MockComponent
    private SecurityRuleAbacus ruleAbacus;

    @MockComponent
    private RightsReader rightsReader;

    @MockComponent
    private UserReferenceConverter userReferenceConverter;

    @MockComponent
    private ChangeRequestConfiguration configuration;

    @MockComponent
    private ApproversManager<ChangeRequest> changeRequestApproversManager;

    @MockComponent
    private DelegateApproverManager<ChangeRequest> changeRequestDelegateApproverManager;

    @Test
    void copyAllButViewRights() throws AuthorizationException, ChangeRequestException, XWikiException
    {
        ChangeRequest sourceChangeRequest = mock(ChangeRequest.class);
        ChangeRequest targetChangeRequest = mock(ChangeRequest.class);

        DocumentReference sourceDocReference = mock(DocumentReference.class);
        DocumentReference targetDocReference = mock(DocumentReference.class);

        when(this.changeRequestDocumentReferenceResolver.resolve(sourceChangeRequest)).thenReturn(sourceDocReference);
        when(this.changeRequestDocumentReferenceResolver.resolve(targetChangeRequest)).thenReturn(targetDocReference);

        SpaceReference sourceSpaceReference = mock(SpaceReference.class);
        when(sourceDocReference.getLastSpaceReference()).thenReturn(sourceSpaceReference);

        SpaceReference targetSpaceReference = mock(SpaceReference.class);
        when(targetDocReference.getLastSpaceReference()).thenReturn(targetSpaceReference);

        ReadableSecurityRule rule1 = mock(ReadableSecurityRule.class);
        ReadableSecurityRule rule2 = mock(ReadableSecurityRule.class);
        ReadableSecurityRule rule3 = mock(ReadableSecurityRule.class);

        when(this.rightsReader.getRules(sourceSpaceReference, false))
            .thenReturn(Arrays.asList(rule1, rule2, rule3));

        when(rule1.match(Right.VIEW)).thenReturn(true);
        when(rule2.match(Right.VIEW)).thenReturn(false);
        when(rule3.match(Right.VIEW)).thenReturn(true);

        WritableSecurityRule rule1bis = mock(WritableSecurityRule.class);
        WritableSecurityRule rule3bis = mock(WritableSecurityRule.class);

        when(this.rightsWriter.createRule(rule1)).thenReturn(rule1bis);
        when(this.rightsWriter.createRule(rule3)).thenReturn(rule3bis);

        when(rule1bis.getRights()).thenReturn(new RightSet(Right.VIEW));
        when(rule3bis.getRights()).thenReturn(new RightSet(Right.VIEW, Right.EDIT));

        this.rightsManager.copyAllButViewRights(sourceChangeRequest, targetChangeRequest);
        verify(rule3bis).setRights(new RightSet(Right.EDIT));
        verify(this.rightsWriter).saveRules(Arrays.asList(rule2, rule3bis), targetSpaceReference);
    }

    @Test
    void isViewAccessConsistent() throws AuthorizationException, ChangeRequestException
    {
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        DocumentReference newChangeReference = mock(DocumentReference.class);

        DocumentReference docRef1 = mock(DocumentReference.class);
        DocumentReference docRef2 = mock(DocumentReference.class);

        when(changeRequest.getModifiedDocuments()).thenReturn(Stream.of(docRef1, docRef2).collect(Collectors.toSet()));

        List<ReadableSecurityRule> rulesDoc1 = mock(List.class);
        List<ReadableSecurityRule> rulesDoc2 = mock(List.class);
        List<ReadableSecurityRule> rulesNewChange = mock(List.class);

        when(this.rightsReader.getActualRules(docRef1)).thenReturn(rulesDoc1);
        ReadableSecurityRule doc1rule1 = mock(ReadableSecurityRule.class);
        when(doc1rule1.match(Right.VIEW)).thenReturn(false);

        when(this.ruleAbacus.normalizeRulesBySubject(rulesDoc1)).thenReturn(Collections.singletonList(doc1rule1));

        when(this.rightsReader.getActualRules(docRef2)).thenReturn(rulesDoc2);
        ReadableSecurityRule doc2rule1 = mock(ReadableSecurityRule.class);
        ReadableSecurityRule doc2rule2 = mock(ReadableSecurityRule.class);
        ReadableSecurityRule doc2rule3 = mock(ReadableSecurityRule.class);

        when(doc2rule1.match(Right.VIEW)).thenReturn(true);
        when(doc2rule2.match(Right.VIEW)).thenReturn(false);
        when(doc2rule3.match(Right.VIEW)).thenReturn(true);

        DocumentReference groupARef = mock(DocumentReference.class);
        DocumentReference groupBRef = mock(DocumentReference.class);
        when(doc2rule1.getGroups()).thenReturn(Arrays.asList(groupARef, groupBRef));

        DocumentReference userFooRef = mock(DocumentReference.class);
        when(doc2rule3.getUsers()).thenReturn(Collections.singletonList(userFooRef));

        when(this.ruleAbacus.normalizeRulesBySubject(rulesDoc2))
            .thenReturn(Arrays.asList(doc2rule1, doc2rule2, doc2rule3));

        when(this.rightsReader.getActualRules(newChangeReference)).thenReturn(rulesNewChange);

        ReadableSecurityRule newChangeRule1 = mock(ReadableSecurityRule.class);
        ReadableSecurityRule newChangeRule2 = mock(ReadableSecurityRule.class);

        when(newChangeRule1.match(Right.VIEW)).thenReturn(true);
        when(newChangeRule2.match(Right.VIEW)).thenReturn(true);

        DocumentReference userBarRef = mock(DocumentReference.class);
        when(newChangeRule1.getUsers()).thenReturn(Arrays.asList(userFooRef, userBarRef));
        when(newChangeRule2.getGroups()).thenReturn(Collections.singletonList(groupBRef));

        when(this.ruleAbacus.normalizeRulesBySubject(rulesNewChange))
            .thenReturn(Arrays.asList(newChangeRule1, newChangeRule2));

        when(this.authorizationManager.hasAccess(Right.VIEW, groupARef, docRef1)).thenReturn(true);
        when(this.authorizationManager.hasAccess(Right.VIEW, groupARef, docRef2)).thenReturn(true);
        when(this.authorizationManager.hasAccess(Right.VIEW, groupARef, newChangeReference)).thenReturn(true);

        when(this.authorizationManager.hasAccess(Right.VIEW, groupBRef, docRef1)).thenReturn(false);
        when(this.authorizationManager.hasAccess(Right.VIEW, groupBRef, docRef2)).thenReturn(false);
        when(this.authorizationManager.hasAccess(Right.VIEW, groupBRef, newChangeReference)).thenReturn(false);

        when(this.authorizationManager.hasAccess(Right.VIEW, userFooRef, docRef1)).thenReturn(false);
        when(this.authorizationManager.hasAccess(Right.VIEW, userFooRef, docRef2)).thenReturn(false);
        when(this.authorizationManager.hasAccess(Right.VIEW, userFooRef, newChangeReference)).thenReturn(false);

        when(this.authorizationManager.hasAccess(Right.VIEW, userBarRef, docRef1)).thenReturn(true);
        when(this.authorizationManager.hasAccess(Right.VIEW, userBarRef, docRef2)).thenReturn(true);
        when(this.authorizationManager.hasAccess(Right.VIEW, userBarRef, newChangeReference)).thenReturn(true);

        assertTrue(this.rightsManager.isViewAccessConsistent(changeRequest, newChangeReference));

        when(this.authorizationManager.hasAccess(Right.VIEW, userBarRef, docRef2)).thenReturn(false);

        assertFalse(this.rightsManager.isViewAccessConsistent(changeRequest, newChangeReference));
    }

    @Test
    void isViewAccessStillConsistent() throws ChangeRequestException
    {
        ChangeRequest changeRequest = mock(ChangeRequest.class);

        DocumentReference doc1 = mock(DocumentReference.class);
        DocumentReference doc2 = mock(DocumentReference.class);
        DocumentReference doc3 = mock(DocumentReference.class);

        when(changeRequest.getModifiedDocuments()).thenReturn(Stream.of(doc1, doc2, doc3).collect(Collectors.toSet()));

        DocumentReference user1 = mock(DocumentReference.class);
        DocumentReference user2 = mock(DocumentReference.class);
        DocumentReference user3 = mock(DocumentReference.class);
        Set<DocumentReference> userSet = Stream.of(user1, user2, user3).collect(Collectors.toSet());

        when(this.authorizationManager.hasAccess(Right.VIEW, user1, doc1)).thenReturn(true);
        when(this.authorizationManager.hasAccess(Right.VIEW, user1, doc2)).thenReturn(true);
        when(this.authorizationManager.hasAccess(Right.VIEW, user1, doc3)).thenReturn(true);

        when(this.authorizationManager.hasAccess(Right.VIEW, user2, doc1)).thenReturn(true);
        when(this.authorizationManager.hasAccess(Right.VIEW, user2, doc2)).thenReturn(true);
        when(this.authorizationManager.hasAccess(Right.VIEW, user2, doc3)).thenReturn(true);

        when(this.authorizationManager.hasAccess(Right.VIEW, user3, doc1)).thenReturn(true);
        when(this.authorizationManager.hasAccess(Right.VIEW, user3, doc2)).thenReturn(true);
        when(this.authorizationManager.hasAccess(Right.VIEW, user3, doc3)).thenReturn(true);

        assertTrue(this.rightsManager.isViewAccessStillConsistent(changeRequest, userSet));

        when(this.authorizationManager.hasAccess(Right.VIEW, user1, doc1)).thenReturn(false);
        assertFalse(this.rightsManager.isViewAccessStillConsistent(changeRequest, userSet));

        when(this.authorizationManager.hasAccess(Right.VIEW, user1, doc2)).thenReturn(false);
        when(this.authorizationManager.hasAccess(Right.VIEW, user1, doc3)).thenReturn(false);
        assertTrue(this.rightsManager.isViewAccessStillConsistent(changeRequest, userSet));

        when(this.authorizationManager.hasAccess(Right.VIEW, user1, doc2)).thenReturn(false);
        when(this.authorizationManager.hasAccess(Right.VIEW, user2, doc2)).thenReturn(false);
        when(this.authorizationManager.hasAccess(Right.VIEW, user3, doc2)).thenReturn(false);
        assertFalse(this.rightsManager.isViewAccessStillConsistent(changeRequest, userSet));

        when(this.authorizationManager.hasAccess(Right.VIEW, user1, doc1)).thenReturn(false);
        when(this.authorizationManager.hasAccess(Right.VIEW, user1, doc2)).thenReturn(false);
        when(this.authorizationManager.hasAccess(Right.VIEW, user1, doc3)).thenReturn(false);

        when(this.authorizationManager.hasAccess(Right.VIEW, user2, doc1)).thenReturn(false);
        when(this.authorizationManager.hasAccess(Right.VIEW, user2, doc2)).thenReturn(false);
        when(this.authorizationManager.hasAccess(Right.VIEW, user2, doc3)).thenReturn(false);

        when(this.authorizationManager.hasAccess(Right.VIEW, user3, doc1)).thenReturn(false);
        when(this.authorizationManager.hasAccess(Right.VIEW, user3, doc2)).thenReturn(false);
        when(this.authorizationManager.hasAccess(Right.VIEW, user3, doc3)).thenReturn(false);

        assertTrue(this.rightsManager.isViewAccessStillConsistent(changeRequest, userSet));

        when(this.authorizationManager.hasAccess(Right.VIEW, user3, doc3)).thenReturn(true);
        assertFalse(this.rightsManager.isViewAccessStillConsistent(changeRequest, userSet));
    }

    @Test
    void copyViewRights() throws AuthorizationException, ChangeRequestException, XWikiException
    {
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        DocumentReference changeRequestDocReference = mock(DocumentReference.class);
        SpaceReference changeRequestSpaceReference = mock(SpaceReference.class);

        DocumentReference newChange = mock(DocumentReference.class);
        WikiReference wikiReference = new WikiReference("foo");
        when(newChange.getWikiReference()).thenReturn(wikiReference);

        when(this.changeRequestDocumentReferenceResolver.resolve(changeRequest)).thenReturn(changeRequestDocReference);
        when(changeRequestDocReference.getLastSpaceReference()).thenReturn(changeRequestSpaceReference);

        WritableSecurityRule crRule1 = mock(WritableSecurityRule.class);
        WritableSecurityRule crRule2 = mock(WritableSecurityRule.class);
        WritableSecurityRule crRule3 = mock(WritableSecurityRule.class);

        List<ReadableSecurityRule> crRules = mock(List.class);
        when(this.rightsReader.getRules(changeRequestSpaceReference, false))
            .thenReturn(crRules);
        when(this.rightsWriter.createRules(crRules)).thenReturn(Arrays.asList(crRule1, crRule2, crRule3));

        ReadableSecurityRule newChangeRule1 = mock(ReadableSecurityRule.class);
        ReadableSecurityRule newChangeRule2 = mock(ReadableSecurityRule.class);
        ReadableSecurityRule newChangeRule3 = mock(ReadableSecurityRule.class);

        when(this.rightsReader.getActualRules(newChange))
            .thenReturn(Arrays.asList(newChangeRule1, newChangeRule2, newChangeRule3));

        ReadableSecurityRule wikiRule1 = mock(ReadableSecurityRule.class);
        when(this.rightsReader.getActualRules(wikiReference)).thenReturn(Arrays.asList(wikiRule1, newChangeRule2));

        when(newChangeRule1.match(Right.VIEW)).thenReturn(false);
        when(newChangeRule3.match(Right.VIEW)).thenReturn(true);

        WritableSecurityRule rule3bis = mock(WritableSecurityRule.class);
        when(this.rightsWriter.createRule(newChangeRule3)).thenReturn(rule3bis);

        this.rightsManager.copyViewRights(changeRequest, newChange);
        verify(rule3bis).setRights(Collections.singletonList(Right.VIEW));
        verify(this.rightsWriter).saveRules(Arrays.asList(crRule1, crRule2, crRule3, rule3bis),
            changeRequestSpaceReference);
    }

    @Test
    void applyChanges() throws AuthorizationException, ChangeRequestException, XWikiException
    {
        // Scenario:
        // Change request containing following rights:
        //   - Allow view,edit,script on XWiki.AdminGroup
        //   - Deny view,edit on XWiki.Foo
        //   - Deny edit on XWiki.Bar
        //   - Allow view on XWiki.AllGroup
        //
        // Diff contains following changes:
        //   - Update to remove allow view on XWiki.AdminGroup
        //   - Update Deny view on XWiki.Foo (2 rules: one to remove Deny view, one to add Allow view)
        //   - Update deny edit to allow edit on XWiki.bar (2 rules: one to remove Deny edit, one to add Allow edit)
        //   - Add allow view on XWiki.Buz
        //   - Remove Allow view on XWiki.AllGroup
        //
        // Expected rights after applying:
        //   - Allow edit,script on XWiki.AdminGroup
        //   - Deny edit on XWiki.Foo
        //   - Allow view on XWiki.Foo
        //   - Deny edit on XWiki.Bar
        //   - Allow view on XWiki.Buz

        DocumentReference adminGroupRef = new DocumentReference("xwiki", "XWiki", "AdminGroup");
        DocumentReference allGroupRef = new DocumentReference("xwiki", "XWiki", "AllGroup");

        DocumentReference fooUserRef = new DocumentReference("xwiki", "XWiki", "Foo");
        DocumentReference barUserRef = new DocumentReference("xwiki", "XWiki", "Bar");
        DocumentReference buzUserRef = new DocumentReference("xwiki", "XWiki", "Buz");

        ChangeRequest changeRequest = mock(ChangeRequest.class);

        // diff1: Update to remove allow view on XWiki.AdminGroup
        SecurityRuleDiff diff1 = mock(SecurityRuleDiff.class);

        when(diff1.getChangeType()).thenReturn(SecurityRuleDiff.ChangeType.RULE_UPDATED);
        ReadableSecurityRule previousRule = mock(ReadableSecurityRule.class);
        when(diff1.getPreviousRule()).thenReturn(previousRule);
        when(previousRule.getGroups()).thenReturn(Collections.singletonList(adminGroupRef));
        when(previousRule.getRights()).thenReturn(new RightSet(Arrays.asList(Right.VIEW, Right.EDIT, Right.SCRIPT)));
        when(previousRule.getState()).thenReturn(RuleState.ALLOW);
        when(previousRule.match(Right.VIEW)).thenReturn(true);

        ReadableSecurityRule currentRule = mock(ReadableSecurityRule.class);
        when(diff1.getCurrentRule()).thenReturn(currentRule);
        when(currentRule.getGroups()).thenReturn(Collections.singletonList(adminGroupRef));
        when(currentRule.getRights()).thenReturn(new RightSet(Arrays.asList(Right.EDIT, Right.SCRIPT)));
        when(currentRule.getState()).thenReturn(RuleState.ALLOW);
        when(currentRule.match(Right.VIEW)).thenReturn(false);

        // diff2: Update Deny view on XWiki.Foo (2 rules: one to remove Deny view, one to add Allow view)
        SecurityRuleDiff diff2_1 = mock(SecurityRuleDiff.class);

        when(diff2_1.getChangeType()).thenReturn(SecurityRuleDiff.ChangeType.RULE_UPDATED);
        previousRule = mock(ReadableSecurityRule.class);
        when(diff2_1.getPreviousRule()).thenReturn(previousRule);
        when(previousRule.getUsers()).thenReturn(Collections.singletonList(fooUserRef));
        when(previousRule.getRights()).thenReturn(new RightSet(Arrays.asList(Right.VIEW, Right.EDIT)));
        when(previousRule.getState()).thenReturn(RuleState.DENY);
        when(previousRule.match(Right.VIEW)).thenReturn(true);

        currentRule = mock(ReadableSecurityRule.class);
        when(diff2_1.getCurrentRule()).thenReturn(currentRule);
        when(currentRule.getUsers()).thenReturn(Collections.singletonList(fooUserRef));
        when(currentRule.getRights()).thenReturn(new RightSet(Arrays.asList(Right.EDIT)));
        when(currentRule.getState()).thenReturn(RuleState.DENY);
        when(currentRule.match(Right.VIEW)).thenReturn(false);

        SecurityRuleDiff diff2_2 = mock(SecurityRuleDiff.class);

        when(diff2_2.getChangeType()).thenReturn(SecurityRuleDiff.ChangeType.RULE_ADDED);
        currentRule = mock(ReadableSecurityRule.class);
        when(diff2_2.getCurrentRule()).thenReturn(currentRule);
        when(currentRule.getUsers()).thenReturn(Collections.singletonList(fooUserRef));
        when(currentRule.getRights()).thenReturn(new RightSet(Arrays.asList(Right.VIEW)));
        when(currentRule.getState()).thenReturn(RuleState.ALLOW);
        when(currentRule.match(Right.VIEW)).thenReturn(true);

        // diff3: Update deny edit to allow edit on XWiki.bar (2 rules: one to remove Deny edit, one to add Allow edit)

        SecurityRuleDiff diff3_1 = mock(SecurityRuleDiff.class);

        when(diff3_1.getChangeType()).thenReturn(SecurityRuleDiff.ChangeType.RULE_DELETED);
        previousRule = mock(ReadableSecurityRule.class);
        when(diff3_1.getPreviousRule()).thenReturn(previousRule);
        when(previousRule.getUsers()).thenReturn(Collections.singletonList(barUserRef));
        when(previousRule.getRights()).thenReturn(new RightSet(Arrays.asList(Right.EDIT)));
        when(previousRule.getState()).thenReturn(RuleState.DENY);
        when(previousRule.match(Right.VIEW)).thenReturn(false);

        SecurityRuleDiff diff3_2 = mock(SecurityRuleDiff.class);

        when(diff3_2.getChangeType()).thenReturn(SecurityRuleDiff.ChangeType.RULE_ADDED);
        currentRule = mock(ReadableSecurityRule.class);
        when(diff3_2.getCurrentRule()).thenReturn(currentRule);
        when(currentRule.getUsers()).thenReturn(Collections.singletonList(barUserRef));
        when(currentRule.getRights()).thenReturn(new RightSet(Arrays.asList(Right.EDIT)));
        when(currentRule.getState()).thenReturn(RuleState.ALLOW);
        when(currentRule.match(Right.VIEW)).thenReturn(false);

        // diff4: Add allow view on XWiki.Buz

        SecurityRuleDiff diff4 = mock(SecurityRuleDiff.class);

        when(diff4.getChangeType()).thenReturn(SecurityRuleDiff.ChangeType.RULE_ADDED);
        currentRule = mock(ReadableSecurityRule.class);
        when(diff4.getCurrentRule()).thenReturn(currentRule);
        when(currentRule.getUsers()).thenReturn(Collections.singletonList(buzUserRef));
        when(currentRule.getRights()).thenReturn(new RightSet(Arrays.asList(Right.VIEW)));
        when(currentRule.getState()).thenReturn(RuleState.ALLOW);
        when(currentRule.match(Right.VIEW)).thenReturn(true);

        // diff5: Remove Allow view on XWiki.AllGroup
        SecurityRuleDiff diff5 = mock(SecurityRuleDiff.class);

        when(diff5.getChangeType()).thenReturn(SecurityRuleDiff.ChangeType.RULE_DELETED);
        previousRule = mock(ReadableSecurityRule.class);
        when(diff5.getPreviousRule()).thenReturn(previousRule);
        when(previousRule.getGroups()).thenReturn(Collections.singletonList(allGroupRef));
        when(previousRule.getRights()).thenReturn(new RightSet(Arrays.asList(Right.VIEW)));
        when(previousRule.getState()).thenReturn(RuleState.ALLOW);
        when(previousRule.match(Right.VIEW)).thenReturn(true);

        DocumentReference changeRequestDocRef = mock(DocumentReference.class);
        SpaceReference changeRequestSpaceRef = mock(SpaceReference.class);
        when(changeRequestDocRef.getLastSpaceReference()).thenReturn(changeRequestSpaceRef);
        when(this.changeRequestDocumentReferenceResolver.resolve(changeRequest)).thenReturn(changeRequestDocRef);

        List rules = mock(List.class);
        when(this.rightsReader.getRules(changeRequestSpaceRef, false)).thenReturn(rules);

        List<ReadableSecurityRule> normalizedRules = new ArrayList<>();

        // rule1: Allow view,edit,script on XWiki.AdminGroup
        ReadableSecurityRule rule1 = mock(ReadableSecurityRule.class);
        when(rule1.getRights()).thenReturn(new RightSet(Arrays.asList(Right.VIEW, Right.EDIT, Right.SCRIPT)));
        when(rule1.getState()).thenReturn(RuleState.ALLOW);
        when(rule1.getGroups()).thenReturn(Arrays.asList(adminGroupRef));
        normalizedRules.add(rule1);

        // rule2: Deny view,edit on XWiki.Foo
        ReadableSecurityRule rule2 = mock(ReadableSecurityRule.class);
        when(rule2.getRights()).thenReturn(new RightSet(Arrays.asList(Right.VIEW, Right.EDIT)));
        when(rule2.getState()).thenReturn(RuleState.DENY);
        when(rule2.getUsers()).thenReturn(Arrays.asList(fooUserRef));
        normalizedRules.add(rule2);

        // rule3: Deny edit on XWiki.Bar
        ReadableSecurityRule rule3 = mock(ReadableSecurityRule.class);
        when(rule3.getRights()).thenReturn(new RightSet(Arrays.asList(Right.EDIT)));
        when(rule3.getState()).thenReturn(RuleState.DENY);
        when(rule3.getUsers()).thenReturn(Arrays.asList(barUserRef));
        normalizedRules.add(rule3);

        // rule4: Allow view on XWiki.AllGroup
        ReadableSecurityRule rule4 = mock(ReadableSecurityRule.class);
        when(rule4.getRights()).thenReturn(new RightSet(Arrays.asList(Right.VIEW)));
        when(rule4.getState()).thenReturn(RuleState.ALLOW);
        when(rule4.getGroups()).thenReturn(Arrays.asList(allGroupRef));
        normalizedRules.add(rule4);

        when(this.ruleAbacus.normalizeRulesBySubject(rules)).thenReturn(normalizedRules);
        when(this.rightsWriter.createRule(any())).thenAnswer(invocationOnMock -> {
            ReadableSecurityRule readableSecurityRule = invocationOnMock.getArgument(0);
            return new WritableSecurityRuleImpl(
                readableSecurityRule.getGroups(),
                readableSecurityRule.getUsers(),
                readableSecurityRule.getRights(),
                readableSecurityRule.getState());
        });
        when(this.rightsWriter.createRule()).thenAnswer(invocationOnMock -> new WritableSecurityRuleImpl());

        // expected1: Allow edit,script on XWiki.AdminGroup
        WritableSecurityRule expected1 = new WritableSecurityRuleImpl(
            Collections.singletonList(adminGroupRef),
            Collections.emptyList(),
            new RightSet(Arrays.asList(Right.EDIT, Right.SCRIPT)),
            RuleState.ALLOW
        );

        // expected2: Deny edit on XWiki.Foo
        WritableSecurityRule expected2 = new WritableSecurityRuleImpl(
            Collections.emptyList(),
            Collections.singletonList(fooUserRef),
            new RightSet(Arrays.asList(Right.EDIT)),
            RuleState.DENY
        );

        // expected3: Allow view on XWiki.Foo
        WritableSecurityRule expected3 = new WritableSecurityRuleImpl(
            Collections.emptyList(),
            Collections.singletonList(fooUserRef),
            new RightSet(Arrays.asList(Right.VIEW)),
            RuleState.ALLOW
        );

        // expected4: Deny edit on XWiki.Bar
        // this one should not be created but that should be still rule3

        // expected5: Allow view on XWiki.Buz
        WritableSecurityRule expected5 = new WritableSecurityRuleImpl(
            Collections.emptyList(),
            Collections.singletonList(buzUserRef),
            new RightSet(Arrays.asList(Right.VIEW)),
            RuleState.ALLOW
        );

        doAnswer(invocationOnMock -> {
            List<ReadableSecurityRule> updatedRules = invocationOnMock.getArgument(0);
            assertTrue(updatedRules.contains(expected1),
                String.format("rule [%s] seems missing from [%s]", expected1, updatedRules));
            assertTrue(updatedRules.contains(expected2),
                String.format("rule [%s] seems missing from [%s]", expected2, updatedRules));
            assertTrue(updatedRules.contains(expected3),
                String.format("rule [%s] seems missing from [%s]", expected3, updatedRules));
            assertTrue(updatedRules.contains(rule3),
                String.format("rule [%s] seems missing from [%s]", rule3, updatedRules));
            assertTrue(updatedRules.contains(expected5),
                String.format("rule [%s] seems missing from [%s]", expected5, updatedRules));
            return null;
        }).when(this.rightsWriter).saveRules(any(), eq(changeRequestSpaceRef));
        this.rightsManager.applyChanges(changeRequest, Arrays.asList(
            diff1,
            diff2_1, diff2_2,
            diff3_1, diff3_2,
            diff4,
            diff5));
        verify(this.rightsWriter).saveRules(any(), eq(changeRequestSpaceRef));
    }

    @Test
    void isAuthorizedToMergeWithoutMergeUser() throws ChangeRequestException
    {
        UserReference userReference = mock(UserReference.class);
        ChangeRequest changeRequest = mock(ChangeRequest.class);

        DocumentReference userDocReference = mock(DocumentReference.class);
        when(this.userReferenceConverter.convert(userReference)).thenReturn(userDocReference);
        when(this.configuration.getMergeUser()).thenReturn(GuestUserReference.INSTANCE);

        FileChange fileChange1 = mock(FileChange.class);
        FileChange fileChange2 = mock(FileChange.class);
        when(fileChange1.getType()).thenReturn(FileChange.FileChangeType.EDITION);
        when(fileChange2.getType()).thenReturn(FileChange.FileChangeType.DELETION);

        DocumentReference reference1 = mock(DocumentReference.class);
        DocumentReference reference2 = mock(DocumentReference.class);
        when(fileChange1.getTargetEntity()).thenReturn(reference1);
        when(fileChange2.getTargetEntity()).thenReturn(reference2);

        when(changeRequest.getLastFileChanges()).thenReturn(Arrays.asList(fileChange1, fileChange2));

        when(this.changeRequestApproversManager.isApprover(userReference, changeRequest, false)).thenReturn(false);
        when(this.authorizationManager.hasAccess(Right.EDIT, userDocReference, reference1)).thenReturn(false);
        when(this.authorizationManager.hasAccess(Right.DELETE, userDocReference, reference2)).thenReturn(false);

        assertFalse(this.rightsManager.isAuthorizedToMerge(userReference, changeRequest));

        when(this.changeRequestApproversManager.isApprover(userReference, changeRequest, false)).thenReturn(true);
        assertFalse(this.rightsManager.isAuthorizedToMerge(userReference, changeRequest));

        when(this.authorizationManager.hasAccess(Right.EDIT, userDocReference, reference1)).thenReturn(true);
        assertFalse(this.rightsManager.isAuthorizedToMerge(userReference, changeRequest));

        when(this.authorizationManager.hasAccess(Right.EDIT, userDocReference, reference2)).thenReturn(true);
        assertFalse(this.rightsManager.isAuthorizedToMerge(userReference, changeRequest));

        when(this.authorizationManager.hasAccess(Right.DELETE, userDocReference, reference2)).thenReturn(true);
        assertTrue(this.rightsManager.isAuthorizedToMerge(userReference, changeRequest));

        when(this.authorizationManager.hasAccess(Right.EDIT, userDocReference, reference2)).thenReturn(false);
        assertTrue(this.rightsManager.isAuthorizedToMerge(userReference, changeRequest));
    }

    @Test
    void isAuthorizedToMergeWithMergeUser() throws ChangeRequestException
    {
        ChangeRequest changeRequest = mock(ChangeRequest.class);

        UserReference currentUserReference = mock(UserReference.class);
        DocumentReference currentUserDocReference = mock(DocumentReference.class);
        when(this.userReferenceConverter.convert(currentUserReference)).thenReturn(currentUserDocReference);

        UserReference mergeUserReference = mock(UserReference.class);
        DocumentReference mergeUserDocReference = mock(DocumentReference.class);
        when(this.userReferenceConverter.convert(mergeUserReference)).thenReturn(mergeUserDocReference);
        when(this.configuration.getMergeUser()).thenReturn(mergeUserReference);

        FileChange fileChange1 = mock(FileChange.class);
        FileChange fileChange2 = mock(FileChange.class);
        when(fileChange1.getType()).thenReturn(FileChange.FileChangeType.EDITION);
        when(fileChange2.getType()).thenReturn(FileChange.FileChangeType.DELETION);

        DocumentReference reference1 = mock(DocumentReference.class);
        DocumentReference reference2 = mock(DocumentReference.class);
        when(fileChange1.getTargetEntity()).thenReturn(reference1);
        when(fileChange2.getTargetEntity()).thenReturn(reference2);

        when(changeRequest.getLastFileChanges()).thenReturn(Arrays.asList(fileChange1, fileChange2));

        when(this.changeRequestApproversManager.isApprover(currentUserReference, changeRequest, false))
            .thenReturn(false);
        when(this.authorizationManager.hasAccess(Right.EDIT, mergeUserDocReference, reference1)).thenReturn(false);
        when(this.authorizationManager.hasAccess(Right.DELETE, mergeUserDocReference, reference2)).thenReturn(false);

        assertFalse(this.rightsManager.isAuthorizedToMerge(currentUserReference, changeRequest));

        when(this.changeRequestApproversManager.isApprover(currentUserReference, changeRequest, false))
            .thenReturn(true);
        assertFalse(this.rightsManager.isAuthorizedToMerge(currentUserReference, changeRequest));

        when(this.authorizationManager.hasAccess(Right.EDIT, mergeUserDocReference, reference1)).thenReturn(true);
        assertFalse(this.rightsManager.isAuthorizedToMerge(currentUserReference, changeRequest));

        when(this.authorizationManager.hasAccess(Right.EDIT, mergeUserDocReference, reference2)).thenReturn(true);
        assertFalse(this.rightsManager.isAuthorizedToMerge(currentUserReference, changeRequest));

        when(this.authorizationManager.hasAccess(Right.DELETE, mergeUserDocReference, reference2)).thenReturn(true);
        assertTrue(this.rightsManager.isAuthorizedToMerge(currentUserReference, changeRequest));

        when(this.authorizationManager.hasAccess(Right.EDIT, mergeUserDocReference, reference2)).thenReturn(false);
        assertTrue(this.rightsManager.isAuthorizedToMerge(currentUserReference, changeRequest));

        verify(this.authorizationManager, never())
            .hasAccess(eq(Right.EDIT), eq(currentUserDocReference), any(DocumentReference.class));
    }

    @Test
    void isAuthorizedToEdit()
    {
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        UserReference userReference = mock(UserReference.class);

        when(changeRequest.getStatus()).thenReturn(ChangeRequestStatus.MERGED);
        when(changeRequest.getAuthors())
            .thenReturn(new HashSet<>(Arrays.asList(userReference, mock(UserReference.class))));
        assertFalse(this.rightsManager.isAuthorizedToEdit(userReference, changeRequest));

        when(changeRequest.getStatus()).thenReturn(ChangeRequestStatus.DRAFT);
        assertTrue(this.rightsManager.isAuthorizedToEdit(userReference, changeRequest));

        when(changeRequest.getAuthors()).thenReturn(Collections.singleton(mock(UserReference.class)));
        assertFalse(this.rightsManager.isAuthorizedToEdit(userReference, changeRequest));

        when(changeRequest.getAuthors()).thenReturn(Collections.singleton(GuestUserReference.INSTANCE));
        assertTrue(this.rightsManager.isAuthorizedToEdit(userReference, changeRequest));

        when(changeRequest.getAuthors()).thenReturn(Collections.singleton(mock(UserReference.class)));
        DocumentReference userDocReference = mock(DocumentReference.class);
        DocumentReference changeRequestDoc = mock(DocumentReference.class);

        when(this.userReferenceConverter.convert(userReference)).thenReturn(userDocReference);
        when(this.changeRequestDocumentReferenceResolver.resolve(changeRequest)).thenReturn(changeRequestDoc);
        when(this.authorizationManager.hasAccess(Right.ADMIN, userDocReference, changeRequestDoc)).thenReturn(false);
        assertFalse(this.rightsManager.isAuthorizedToEdit(userReference, changeRequest));

        when(this.authorizationManager.hasAccess(Right.ADMIN, userDocReference, changeRequestDoc)).thenReturn(true);
        assertTrue(this.rightsManager.isAuthorizedToEdit(userReference, changeRequest));
    }

    @Test
    void isAuthorizedToOpen()
    {
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        UserReference userReference = mock(UserReference.class);

        when(changeRequest.getStatus()).thenReturn(ChangeRequestStatus.MERGED);
        when(changeRequest.getAuthors())
            .thenReturn(new HashSet<>(Arrays.asList(userReference, mock(UserReference.class))));
        assertFalse(this.rightsManager.isAuthorizedToOpen(userReference, changeRequest));

        when(changeRequest.getStatus()).thenReturn(ChangeRequestStatus.CLOSED);
        assertTrue(this.rightsManager.isAuthorizedToOpen(userReference, changeRequest));

        when(changeRequest.getAuthors()).thenReturn(Collections.singleton(mock(UserReference.class)));
        DocumentReference userDocReference = mock(DocumentReference.class);
        DocumentReference changeRequestDoc = mock(DocumentReference.class);

        when(this.userReferenceConverter.convert(userReference)).thenReturn(userDocReference);
        when(this.changeRequestDocumentReferenceResolver.resolve(changeRequest)).thenReturn(changeRequestDoc);
        when(this.authorizationManager.hasAccess(Right.ADMIN, userDocReference, changeRequestDoc)).thenReturn(false);
        assertFalse(this.rightsManager.isAuthorizedToOpen(userReference, changeRequest));

        when(this.authorizationManager.hasAccess(Right.ADMIN, userDocReference, changeRequestDoc)).thenReturn(true);
        assertTrue(this.rightsManager.isAuthorizedToOpen(userReference, changeRequest));
    }

    @Test
    void isAuthorizedToReview() throws ChangeRequestException
    {
        UserReference userReference = mock(UserReference.class);
        ChangeRequest changeRequest = mock(ChangeRequest.class);

        when(changeRequest.getAuthors()).thenReturn(new HashSet<>(List.of(userReference, mock(UserReference.class))));
        when(configuration.preventAuthorToReview()).thenReturn(true);
        assertFalse(this.rightsManager.isAuthorizedToReview(userReference, changeRequest));
        verifyNoInteractions(this.changeRequestApproversManager);

        when(configuration.preventAuthorToReview()).thenReturn(false);
        when(this.changeRequestApproversManager.isApprover(userReference, changeRequest, false)).thenReturn(false);
        assertFalse(this.rightsManager.isAuthorizedToReview(userReference, changeRequest));

        when(this.changeRequestApproversManager.isApprover(userReference, changeRequest, false)).thenReturn(true);
        assertTrue(this.rightsManager.isAuthorizedToReview(userReference, changeRequest));

        when(changeRequest.getAuthors()).thenReturn(Collections.singleton(mock(UserReference.class)));
        when(configuration.preventAuthorToReview()).thenReturn(true);
        when(this.changeRequestApproversManager.isApprover(userReference, changeRequest, false)).thenReturn(false);
        assertFalse(this.rightsManager.isAuthorizedToReview(userReference, changeRequest));

        when(this.changeRequestApproversManager.isApprover(userReference, changeRequest, false)).thenReturn(true);
        assertTrue(this.rightsManager.isAuthorizedToReview(userReference, changeRequest));

        when(configuration.preventAuthorToReview()).thenReturn(false);
        when(this.changeRequestApproversManager.isApprover(userReference, changeRequest, false)).thenReturn(false);
        assertFalse(this.rightsManager.isAuthorizedToReview(userReference, changeRequest));

        when(this.changeRequestApproversManager.isApprover(userReference, changeRequest, false)).thenReturn(true);
        assertTrue(this.rightsManager.isAuthorizedToReview(userReference, changeRequest));
    }

    @Test
    void isAuthorizedToReviewOnBehalf() throws ChangeRequestException
    {
        UserReference userReference = mock(UserReference.class);
        UserReference originalApprover = mock(UserReference.class);
        ChangeRequest changeRequest = mock(ChangeRequest.class);

        when(changeRequest.getAuthors()).thenReturn(new HashSet<>(List.of(userReference, mock(UserReference.class))));
        when(configuration.preventAuthorToReview()).thenReturn(true);
        assertFalse(this.rightsManager.isAuthorizedToReviewOnBehalf(userReference, changeRequest, originalApprover));
        verifyNoInteractions(this.changeRequestDelegateApproverManager);

        when(configuration.preventAuthorToReview()).thenReturn(false);
        when(this.changeRequestDelegateApproverManager
            .isDelegateApproverOf(userReference, changeRequest, originalApprover)).thenReturn(false);
        assertFalse(this.rightsManager.isAuthorizedToReviewOnBehalf(userReference, changeRequest, originalApprover));

        when(this.changeRequestDelegateApproverManager
            .isDelegateApproverOf(userReference, changeRequest, originalApprover)).thenReturn(true);
        assertTrue(this.rightsManager.isAuthorizedToReviewOnBehalf(userReference, changeRequest, originalApprover));

        when(changeRequest.getAuthors()).thenReturn(Collections.singleton(mock(UserReference.class)));
        when(configuration.preventAuthorToReview()).thenReturn(true);
        when(this.changeRequestDelegateApproverManager
            .isDelegateApproverOf(userReference, changeRequest, originalApprover)).thenReturn(false);
        assertFalse(this.rightsManager.isAuthorizedToReviewOnBehalf(userReference, changeRequest, originalApprover));

        when(this.changeRequestDelegateApproverManager
            .isDelegateApproverOf(userReference, changeRequest, originalApprover)).thenReturn(true);
        assertTrue(this.rightsManager.isAuthorizedToReviewOnBehalf(userReference, changeRequest, originalApprover));

        when(configuration.preventAuthorToReview()).thenReturn(false);
        when(this.changeRequestDelegateApproverManager
            .isDelegateApproverOf(userReference, changeRequest, originalApprover)).thenReturn(false);
        assertFalse(this.rightsManager.isAuthorizedToReviewOnBehalf(userReference, changeRequest, originalApprover));

        when(this.changeRequestDelegateApproverManager
            .isDelegateApproverOf(userReference, changeRequest, originalApprover)).thenReturn(true);
        assertTrue(this.rightsManager.isAuthorizedToReviewOnBehalf(userReference, changeRequest, originalApprover));
    }

    @Test
    void isAuthorizedToReviewAsDelegate() throws ChangeRequestException
    {
        UserReference userReference = mock(UserReference.class);
        ChangeRequest changeRequest = mock(ChangeRequest.class);

        when(changeRequest.getAuthors()).thenReturn(new HashSet<>(List.of(userReference, mock(UserReference.class))));
        when(configuration.preventAuthorToReview()).thenReturn(true);
        assertFalse(this.rightsManager.isAuthorizedToReviewAsDelegate(userReference, changeRequest));
        verifyNoInteractions(this.changeRequestDelegateApproverManager);

        when(configuration.preventAuthorToReview()).thenReturn(false);
        when(this.changeRequestDelegateApproverManager
            .isDelegateApproverOf(userReference, changeRequest)).thenReturn(false);
        assertFalse(this.rightsManager.isAuthorizedToReviewAsDelegate(userReference, changeRequest));

        when(this.changeRequestDelegateApproverManager
            .isDelegateApproverOf(userReference, changeRequest)).thenReturn(true);
        assertTrue(this.rightsManager.isAuthorizedToReviewAsDelegate(userReference, changeRequest));

        when(changeRequest.getAuthors()).thenReturn(Collections.singleton(mock(UserReference.class)));
        when(configuration.preventAuthorToReview()).thenReturn(true);
        when(this.changeRequestDelegateApproverManager
            .isDelegateApproverOf(userReference, changeRequest)).thenReturn(false);
        assertFalse(this.rightsManager.isAuthorizedToReviewAsDelegate(userReference, changeRequest));

        when(this.changeRequestDelegateApproverManager
            .isDelegateApproverOf(userReference, changeRequest)).thenReturn(true);
        assertTrue(this.rightsManager.isAuthorizedToReviewAsDelegate(userReference, changeRequest));

        when(configuration.preventAuthorToReview()).thenReturn(false);
        when(this.changeRequestDelegateApproverManager
            .isDelegateApproverOf(userReference, changeRequest)).thenReturn(false);
        assertFalse(this.rightsManager.isAuthorizedToReviewAsDelegate(userReference, changeRequest));

        when(this.changeRequestDelegateApproverManager
            .isDelegateApproverOf(userReference, changeRequest)).thenReturn(true);
        assertTrue(this.rightsManager.isAuthorizedToReviewAsDelegate(userReference, changeRequest));
    }

    @Test
    void isAuthorizedToComment() throws ChangeRequestException
    {
        UserReference userReference = mock(UserReference.class);
        ChangeRequest changeRequest = mock(ChangeRequest.class);

        DocumentReference userDocReference = mock(DocumentReference.class);
        DocumentReference changeRequestDoc = mock(DocumentReference.class);
        when(this.userReferenceConverter.convert(userReference)).thenReturn(userDocReference);
        when(this.changeRequestDocumentReferenceResolver.resolve(changeRequest)).thenReturn(changeRequestDoc);
        when(this.authorizationManager.hasAccess(Right.ADMIN, userDocReference, changeRequestDoc)).thenReturn(true);
        assertTrue(this.rightsManager.isAuthorizedToComment(userReference, changeRequest));

        when(this.authorizationManager.hasAccess(Right.ADMIN, userDocReference, changeRequestDoc)).thenReturn(false);
        when(this.authorizationManager.hasAccess(Right.COMMENT, userDocReference, changeRequestDoc)).thenReturn(true);
        assertTrue(this.rightsManager.isAuthorizedToComment(userReference, changeRequest));

        when(this.authorizationManager.hasAccess(Right.COMMENT, userDocReference, changeRequestDoc)).thenReturn(false);
        when(configuration.preventAuthorToReview()).thenReturn(false);
        when(this.changeRequestApproversManager.isApprover(userReference, changeRequest, false)).thenReturn(true);
        assertTrue(this.rightsManager.isAuthorizedToComment(userReference, changeRequest));

        when(this.changeRequestApproversManager.isApprover(userReference, changeRequest, false)).thenReturn(false);
        assertFalse(this.rightsManager.isAuthorizedToComment(userReference, changeRequest));
    }
}
