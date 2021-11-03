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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.rights.RightsReader;
import org.xwiki.contrib.rights.RightsWriter;
import org.xwiki.contrib.rights.SecurityRuleAbacus;
import org.xwiki.contrib.rights.WritableSecurityRule;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.authorization.AuthorizationException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.ReadableSecurityRule;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.RightSet;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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

        when(this.rightsReader.getActualRules(sourceSpaceReference, false))
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
        when(this.rightsReader.getActualRules(changeRequestSpaceReference, false))
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
}
