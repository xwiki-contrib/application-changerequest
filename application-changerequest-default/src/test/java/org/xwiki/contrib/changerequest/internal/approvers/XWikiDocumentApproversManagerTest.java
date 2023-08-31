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

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.contrib.changerequest.internal.UserReferenceConverter;
import org.xwiki.contrib.changerequest.rights.ChangeRequestApproveRight;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.user.UserReferenceSerializer;
import org.xwiki.user.group.GroupManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link XWikiDocumentApproversManagerTest}.
 *
 * @version $Id$
 * @since 0.5
 */
@ComponentTest
class XWikiDocumentApproversManagerTest
{
    @InjectMockComponents
    private XWikiDocumentApproversManager manager;

    @MockComponent
    @Named("current")
    private UserReferenceResolver<String> stringUserReferenceResolver;

    @MockComponent
    private GroupManager groupManager;

    @MockComponent
    @Named("document")
    private UserReferenceResolver<DocumentReference> documentReferenceUserReferenceResolver;

    @MockComponent
    private UserReferenceSerializer<String> userReferenceSerializer;

    @MockComponent
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @MockComponent
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private UserReferenceConverter userReferenceConverter;

    @MockComponent
    private AuthorizationManager authorizationManager;

    @MockComponent
    private UserReferenceResolver<CurrentUserReference> currentUserReferenceUserReferenceResolver;

    private XWikiContext context;
    private XWiki wiki;

    @BeforeEach
    void setup()
    {
        this.context = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(this.context);
        this.wiki = mock(XWiki.class);
        when(this.context.getWiki()).thenReturn(this.wiki);
    }

    @Test
    void getAllApprovers() throws Exception
    {
        XWikiDocument xWikiDocument = mock(XWikiDocument.class);
        assertEquals(new HashSet<>(), this.manager.getAllApprovers(xWikiDocument, true));
        assertEquals(new HashSet<>(), this.manager.getAllApprovers(xWikiDocument, false));

        BaseObject xobject = mock(BaseObject.class);
        when(xWikiDocument.getXObject(ApproversXClassInitializer.APPROVERS_XCLASS, false, this.context))
            .thenReturn(xobject);
        assertEquals(new HashSet<>(), this.manager.getAllApprovers(xWikiDocument, true));
        assertEquals(new HashSet<>(), this.manager.getAllApprovers(xWikiDocument, false));

        when(xobject.getLargeStringValue(ApproversXClassInitializer.USERS_APPROVERS_PROPERTY))
            .thenReturn("Foo,Bar,Buz");
        UserReference user1 = mock(UserReference.class);
        UserReference user2 = mock(UserReference.class);
        UserReference user3 = mock(UserReference.class);

        when(this.stringUserReferenceResolver.resolve("Foo")).thenReturn(user1);
        when(this.stringUserReferenceResolver.resolve("Bar")).thenReturn(user2);
        when(this.stringUserReferenceResolver.resolve("Buz")).thenReturn(user3);

        when(xobject.getLargeStringValue(ApproversXClassInitializer.GROUPS_APPROVERS_PROPERTY))
            .thenReturn("GroupA,GroupB");
        DocumentReference groupARef = mock(DocumentReference.class);
        DocumentReference groupBRef = mock(DocumentReference.class);

        when(this.documentReferenceResolver.resolve("GroupA")).thenReturn(groupARef);
        when(this.documentReferenceResolver.resolve("GroupB")).thenReturn(groupBRef);

        DocumentReference userARef = mock(DocumentReference.class);
        DocumentReference userBRef = mock(DocumentReference.class);
        DocumentReference userB2Ref = mock(DocumentReference.class);
        DocumentReference fooRef = mock(DocumentReference.class);
        DocumentReference userA2Ref = mock(DocumentReference.class);

        when(this.groupManager.getMembers(groupARef, true)).thenReturn(Arrays.asList(userARef, userA2Ref, fooRef));
        when(this.groupManager.getMembers(groupBRef, true)).thenReturn(Arrays.asList(userBRef, userB2Ref));

        UserReference user4 = mock(UserReference.class);
        UserReference user5 = mock(UserReference.class);
        UserReference user6 = mock(UserReference.class);
        UserReference user7 = mock(UserReference.class);

        when(this.documentReferenceUserReferenceResolver.resolve(userARef)).thenReturn(user4);
        when(this.documentReferenceUserReferenceResolver.resolve(userA2Ref)).thenReturn(user5);
        when(this.documentReferenceUserReferenceResolver.resolve(userBRef)).thenReturn(user6);
        when(this.documentReferenceUserReferenceResolver.resolve(userB2Ref)).thenReturn(user7);
        when(this.documentReferenceUserReferenceResolver.resolve(fooRef)).thenReturn(user1);

        assertEquals(new HashSet<>(Arrays.asList(user1, user2, user3)),
            this.manager.getAllApprovers(xWikiDocument, false));
        assertEquals(new HashSet<>(Arrays.asList(user1, user2, user3, user4, user5, user6, user7)),
            this.manager.getAllApprovers(xWikiDocument, true));
    }

    @Test
    void isApprover() throws Exception
    {
        XWikiDocument xWikiDocument = mock(XWikiDocument.class);
        BaseObject xobject = mock(BaseObject.class);
        when(xWikiDocument.getXObject(ApproversXClassInitializer.APPROVERS_XCLASS, false, this.context))
            .thenReturn(xobject);
        when(xobject.getLargeStringValue(ApproversXClassInitializer.USERS_APPROVERS_PROPERTY))
            .thenReturn("Foo,Bar");
        UserReference user1 = mock(UserReference.class);
        UserReference user2 = mock(UserReference.class);
        when(this.stringUserReferenceResolver.resolve("Foo")).thenReturn(user1);
        when(this.stringUserReferenceResolver.resolve("Bar")).thenReturn(user2);

        assertTrue(this.manager.isApprover(user1, xWikiDocument, false));
        assertTrue(this.manager.isApprover(user1, xWikiDocument, true));
        verify(this.authorizationManager, never()).hasAccess(any(), any(), any());

        DocumentReference documentReference = mock(DocumentReference.class);
        when(xWikiDocument.getDocumentReference()).thenReturn(documentReference);
        UserReference user3 = mock(UserReference.class);
        DocumentReference user3DocRef = mock(DocumentReference.class);
        when(this.userReferenceConverter.convert(user3)).thenReturn(user3DocRef);
        when(this.authorizationManager.hasAccess(ChangeRequestApproveRight.getRight(), user3DocRef, documentReference))
            .thenReturn(true);
        assertFalse(this.manager.isApprover(user3, xWikiDocument, false));
        assertFalse(this.manager.isApprover(user3, xWikiDocument, true));
        verify(this.authorizationManager, never()).hasAccess(any(), any(), any());

        when(xWikiDocument.getXObject(ApproversXClassInitializer.APPROVERS_XCLASS, false, this.context))
            .thenReturn(null);

        assertTrue(this.manager.isApprover(user3, xWikiDocument, false));
        assertFalse(this.manager.isApprover(user3, xWikiDocument, true));

        when(this.authorizationManager.hasAccess(ChangeRequestApproveRight.getRight(), user3DocRef, documentReference))
            .thenReturn(false);
        assertFalse(this.manager.isApprover(user3, xWikiDocument, false));
        assertFalse(this.manager.isApprover(user3, xWikiDocument, true));

        when(this.authorizationManager.hasAccess(Right.ADMIN, user3DocRef, documentReference))
            .thenReturn(true);
        assertTrue(this.manager.isApprover(user3, xWikiDocument, false));
        assertFalse(this.manager.isApprover(user3, xWikiDocument, true));

        when(this.currentUserReferenceUserReferenceResolver.resolve(CurrentUserReference.INSTANCE)).thenReturn(user3);
        assertTrue(this.manager.isApprover(CurrentUserReference.INSTANCE, xWikiDocument, false));
        assertFalse(this.manager.isApprover(CurrentUserReference.INSTANCE, xWikiDocument, true));
    }

    @Test
    void setUsersApprovers() throws Exception
    {
        XWikiDocument xWikiDocument = mock(XWikiDocument.class);
        BaseObject xobject = mock(BaseObject.class);
        when(xWikiDocument.getXObject(ApproversXClassInitializer.APPROVERS_XCLASS, true, this.context))
            .thenReturn(xobject);

        UserReference user1 = mock(UserReference.class);
        UserReference user2 = mock(UserReference.class);
        when(this.userReferenceSerializer.serialize(user1)).thenReturn("Foo");
        when(this.userReferenceSerializer.serialize(user2)).thenReturn("Bar");

        when(xobject.getOwnerDocument()).thenReturn(xWikiDocument);
        when(xWikiDocument.isMetaDataDirty()).thenReturn(false);
        this.manager.setUsersApprovers(new LinkedHashSet<>(Arrays.asList(user1, user2)), xWikiDocument);
        verify(xobject).setLargeStringValue(ApproversXClassInitializer.USERS_APPROVERS_PROPERTY, "Foo,Bar");
        verify(this.wiki, never()).saveDocument(xWikiDocument, "Save approvers.", true, this.context);

        when(xWikiDocument.isMetaDataDirty()).thenReturn(true);
        this.manager.setUsersApprovers(new LinkedHashSet<>(Arrays.asList(user1, user2)), xWikiDocument);
        verify(xobject, times(2)).setLargeStringValue(ApproversXClassInitializer.USERS_APPROVERS_PROPERTY, "Foo,Bar");
        verify(this.wiki).saveDocument(xWikiDocument, "Save approvers.", true, this.context);
    }

    @Test
    void setGroupsApprovers() throws Exception
    {
        XWikiDocument xWikiDocument = mock(XWikiDocument.class);
        BaseObject xobject = mock(BaseObject.class);
        when(xWikiDocument.getXObject(ApproversXClassInitializer.APPROVERS_XCLASS, true, this.context))
            .thenReturn(xobject);

        DocumentReference group1 = mock(DocumentReference.class);
        DocumentReference group2 = mock(DocumentReference.class);
        when(this.entityReferenceSerializer.serialize(group1)).thenReturn("GroupA");
        when(this.entityReferenceSerializer.serialize(group2)).thenReturn("GroupB");

        when(xobject.getOwnerDocument()).thenReturn(xWikiDocument);
        when(xWikiDocument.isMetaDataDirty()).thenReturn(false);
        this.manager.setGroupsApprovers(new LinkedHashSet<>(Arrays.asList(group1, group2)), xWikiDocument);
        verify(xobject).setLargeStringValue(ApproversXClassInitializer.GROUPS_APPROVERS_PROPERTY,
            "GroupA,GroupB");
        verify(this.wiki, never()).saveDocument(xWikiDocument, "Save approvers.", true, this.context);

        when(xWikiDocument.isMetaDataDirty()).thenReturn(true);
        this.manager.setGroupsApprovers(new LinkedHashSet<>(Arrays.asList(group1, group2)), xWikiDocument);
        verify(xobject, times(2)).setLargeStringValue(ApproversXClassInitializer.GROUPS_APPROVERS_PROPERTY,
            "GroupA,GroupB");
        verify(this.wiki).saveDocument(xWikiDocument, "Save approvers.", true, this.context);
    }
}
