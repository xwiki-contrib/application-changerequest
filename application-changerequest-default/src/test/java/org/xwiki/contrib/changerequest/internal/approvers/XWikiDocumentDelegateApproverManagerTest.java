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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.contrib.changerequest.ApproversManager;
import org.xwiki.contrib.changerequest.ChangeRequestConfiguration;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.internal.UserReferenceConverter;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.user.UserReferenceSerializer;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.mandatory.XWikiUsersDocumentInitializer;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.LargeStringProperty;
import com.xpn.xwiki.objects.ListProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link XWikiDocumentDelegateApproverManager}.
 *
 * @version $Id$
 * @since 0.13
 */
@ComponentTest
class XWikiDocumentDelegateApproverManagerTest
{
    @InjectMockComponents
    private XWikiDocumentDelegateApproverManager delegateApproverManager;

    @MockComponent
    private ChangeRequestConfiguration configuration;

    @MockComponent
    private UserReferenceConverter userReferenceConverter;

    @MockComponent
    private UserReferenceResolver<String> stringUserReferenceResolver;

    @MockComponent
    private UserReferenceSerializer<String> userReferenceSerializer;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private Provider<ApproversManager<XWikiDocument>> approversManagerProvider;

    @MockComponent
    private CacheManager cacheManager;

    private XWikiContext context;
    private XWiki wiki;
    private ApproversManager<XWikiDocument> approversManager;
    private Cache<Set<UserReference>> delegateCache;

    @BeforeEach
    void beforeEach() throws CacheException, InitializationException
    {
        this.context = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(this.context);

        this.wiki = mock(XWiki.class);
        when(this.context.getWiki()).thenReturn(this.wiki);

        this.approversManager = mock(ApproversManager.class);
        when(this.approversManagerProvider.get()).thenReturn(this.approversManager);

        this.delegateCache = mock(Cache.class);
        when(this.cacheManager.createNewCache(any())).then(invocation -> {
            CacheConfiguration cacheConfiguration = invocation.getArgument(0);
            assertEquals("changerequest.delegate", cacheConfiguration.getConfigurationId());
            return this.delegateCache;
        });
        this.delegateApproverManager.initialize();
    }

    @Test
    void computeDelegates() throws ChangeRequestException, XWikiException
    {
        UserReference inputReference = mock(UserReference.class);
        when(this.configuration.isDelegateEnabled()).thenReturn(false);
        assertEquals(Collections.emptySet(), this.delegateApproverManager.computeDelegates(inputReference));

        when(this.configuration.isDelegateEnabled()).thenReturn(true);
        when(this.configuration.getDelegateClassPropertyList()).thenReturn(Collections.emptyList());
        assertEquals(Collections.emptySet(), this.delegateApproverManager.computeDelegates(inputReference));

        when(this.configuration.getDelegateClassPropertyList()).thenReturn(Arrays.asList("delegate", "manager"));
        DocumentReference userDocRef = mock(DocumentReference.class);
        when(this.userReferenceConverter.convert(inputReference)).thenReturn(userDocRef);

        XWikiDocument userDoc = mock(XWikiDocument.class);
        when(this.wiki.getDocument(userDocRef, this.context)).thenReturn(userDoc);
        BaseObject userObj = mock(BaseObject.class);
        when(userDoc.getXObject(XWikiUsersDocumentInitializer.XWIKI_USERS_DOCUMENT_REFERENCE)).thenReturn(userObj);

        ListProperty delegateProp = mock(ListProperty.class);
        LargeStringProperty managerProp = mock(LargeStringProperty.class);
        when(userObj.get("delegate")).thenReturn(delegateProp);
        when(userObj.get("manager")).thenReturn(managerProp);

        String foo = "XWiki.Foo";
        String bar = "XWiki.Bar";
        String buz = "XWiki.Buz";

        UserReference fooRef = mock(UserReference.class);
        UserReference barRef = mock(UserReference.class);
        UserReference buzRef = mock(UserReference.class);

        when(this.stringUserReferenceResolver.resolve(foo)).thenReturn(fooRef);
        when(this.stringUserReferenceResolver.resolve(bar)).thenReturn(barRef);
        when(this.stringUserReferenceResolver.resolve(buz)).thenReturn(buzRef);

        when(this.userReferenceSerializer.serialize(fooRef)).thenReturn(foo);
        when(this.userReferenceSerializer.serialize(barRef)).thenReturn(bar);
        when(this.userReferenceSerializer.serialize(buzRef)).thenReturn(buz);

        when(this.userReferenceSerializer.serialize(inputReference)).thenReturn("XWiki.Current");

        when(delegateProp.getList()).thenReturn(Arrays.asList(foo, bar));
        when(managerProp.getValue()).thenReturn(String.format("%s,%s,%s", bar, buz, bar));

        when(userDoc.createXObject(DelegateApproversXClassInitializer.DELEGATE_APPROVERS_XCLASS, context))
            .thenReturn(1);
        BaseObject delegateObj = mock(BaseObject.class);
        when(userDoc.getXObject(DelegateApproversXClassInitializer.DELEGATE_APPROVERS_XCLASS, 1))
            .thenReturn(delegateObj);

        doAnswer(invocation -> {
            String value = invocation.getArgument(1);

            // values are coming from a set, we cannot guarantee the order
            assertTrue(value.contains(foo));
            assertTrue(value.contains(bar));
            assertTrue(value.contains(buz));
            return null;
        }).when(delegateObj).setLargeStringValue(eq(DelegateApproversXClassInitializer.DELEGATED_USERS_PROPERTY),
            anyString());

        Set<UserReference> expectedResult = new HashSet<>(List.of(fooRef, barRef, buzRef));
        assertEquals(expectedResult, this.delegateApproverManager.computeDelegates(inputReference));

        verify(userDoc).removeXObjects(DelegateApproversXClassInitializer.DELEGATE_APPROVERS_XCLASS);
        verify(delegateObj).setLargeStringValue(eq(DelegateApproversXClassInitializer.DELEGATED_USERS_PROPERTY),
            anyString());
        verify(wiki).saveDocument(userDoc, "Computation of delegate approvers", context);
        verify(delegateCache).set("XWiki.Current", expectedResult);
    }

    @Test
    void getDelegates() throws ChangeRequestException, XWikiException
    {
        UserReference inputReference = mock(UserReference.class);
        when(this.configuration.isDelegateEnabled()).thenReturn(false);
        assertEquals(Collections.emptySet(), this.delegateApproverManager.getDelegates(inputReference));

        when(this.configuration.isDelegateEnabled()).thenReturn(true);

        String foo = "XWiki.Foo";
        String bar = "XWiki.Bar";
        String buz = "XWiki.Buz";

        UserReference fooRef = mock(UserReference.class);
        UserReference barRef = mock(UserReference.class);
        UserReference buzRef = mock(UserReference.class);

        when(this.stringUserReferenceResolver.resolve(foo)).thenReturn(fooRef);
        when(this.stringUserReferenceResolver.resolve(bar)).thenReturn(barRef);
        when(this.stringUserReferenceResolver.resolve(buz)).thenReturn(buzRef);

        when(this.userReferenceSerializer.serialize(fooRef)).thenReturn(foo);
        when(this.userReferenceSerializer.serialize(barRef)).thenReturn(bar);
        when(this.userReferenceSerializer.serialize(buzRef)).thenReturn(buz);

        when(this.userReferenceSerializer.serialize(inputReference)).thenReturn("XWiki.Current");

        Set<UserReference> expectedSet = new HashSet<>(List.of(fooRef, barRef));
        when(this.delegateCache.get("XWiki.Current")).thenReturn(expectedSet);
        assertEquals(expectedSet, this.delegateApproverManager.getDelegates(inputReference));

        when(this.delegateCache.get("XWiki.Current")).thenReturn(null);
        DocumentReference userDocRef = mock(DocumentReference.class);
        when(this.userReferenceConverter.convert(inputReference)).thenReturn(userDocRef);

        XWikiDocument userDoc = mock(XWikiDocument.class);
        when(this.wiki.getDocument(userDocRef, this.context)).thenReturn(userDoc);
        when(userDoc.getXObject(DelegateApproversXClassInitializer.DELEGATE_APPROVERS_XCLASS)).thenReturn(null);
        assertEquals(Collections.emptySet(), this.delegateApproverManager.getDelegates(inputReference));
        verify(this.delegateCache).set("XWiki.Current", Collections.emptySet());

        BaseObject delegateObj = mock(BaseObject.class);
        when(userDoc.getXObject(DelegateApproversXClassInitializer.DELEGATE_APPROVERS_XCLASS)).thenReturn(delegateObj);
        when(delegateObj.getLargeStringValue(DelegateApproversXClassInitializer.DELEGATED_USERS_PROPERTY))
            .thenReturn(String.format("%s,%s,%s", foo, foo, buz));
        expectedSet = new HashSet<>(List.of(fooRef, buzRef));

        assertEquals(expectedSet, this.delegateApproverManager.getDelegates(inputReference));
        verify(this.delegateCache).set("XWiki.Current", expectedSet);
    }

    @Test
    void isDelegateApproverOf() throws ChangeRequestException
    {
        UserReference inputReference = mock(UserReference.class);
        XWikiDocument document = mock(XWikiDocument.class);
        when(this.configuration.isDelegateEnabled()).thenReturn(false);
        assertFalse(this.delegateApproverManager.isDelegateApproverOf(inputReference, document));

        when(this.configuration.isDelegateEnabled()).thenReturn(true);

        String foo = "XWiki.Foo";
        String bar = "XWiki.Bar";
        String buz = "XWiki.Buz";

        UserReference fooRef = mock(UserReference.class);
        UserReference barRef = mock(UserReference.class);
        UserReference buzRef = mock(UserReference.class);

        when(this.userReferenceSerializer.serialize(fooRef)).thenReturn(foo);
        when(this.userReferenceSerializer.serialize(barRef)).thenReturn(bar);
        when(this.userReferenceSerializer.serialize(buzRef)).thenReturn(buz);

        when(this.approversManager.getAllApprovers(document, false))
            .thenReturn(new HashSet<>(List.of(fooRef, barRef, buzRef)));
        when(this.delegateCache.get(foo)).thenReturn(Collections.singleton(mock(UserReference.class)));
        when(this.delegateCache.get(bar)).thenReturn(Collections.singleton(mock(UserReference.class)));
        when(this.delegateCache.get(buz)).thenReturn(Collections.singleton(mock(UserReference.class)));

        assertFalse(this.delegateApproverManager.isDelegateApproverOf(inputReference, document));

        when(this.delegateCache.get(bar)).thenReturn(Collections.singleton(inputReference));
        assertTrue(this.delegateApproverManager.isDelegateApproverOf(inputReference, document));
    }

    @Test
    void isDelegateApproverOfWithOriginalApprover() throws ChangeRequestException
    {
        UserReference inputReference = mock(UserReference.class);
        XWikiDocument document = mock(XWikiDocument.class);
        when(this.configuration.isDelegateEnabled()).thenReturn(false);
        assertFalse(this.delegateApproverManager
            .isDelegateApproverOf(inputReference, document, mock(UserReference.class)));

        when(this.configuration.isDelegateEnabled()).thenReturn(true);

        String foo = "XWiki.Foo";
        String bar = "XWiki.Bar";
        String buz = "XWiki.Buz";

        UserReference fooRef = mock(UserReference.class);
        UserReference barRef = mock(UserReference.class);
        UserReference buzRef = mock(UserReference.class);

        when(this.userReferenceSerializer.serialize(fooRef)).thenReturn(foo);
        when(this.userReferenceSerializer.serialize(barRef)).thenReturn(bar);
        when(this.userReferenceSerializer.serialize(buzRef)).thenReturn(buz);

        when(this.approversManager.getAllApprovers(document, false))
            .thenReturn(new HashSet<>(List.of(fooRef, barRef)));
        when(this.delegateCache.get(foo)).thenReturn(Collections.singleton(mock(UserReference.class)));
        when(this.delegateCache.get(bar)).thenReturn(Collections.singleton(inputReference));

        assertFalse(this.delegateApproverManager.isDelegateApproverOf(inputReference, document, fooRef));
        assertFalse(this.delegateApproverManager.isDelegateApproverOf(inputReference, document, buzRef));
        assertTrue(this.delegateApproverManager.isDelegateApproverOf(inputReference, document, barRef));
    }

    @Test
    void getOriginalApprovers() throws ChangeRequestException
    {
        UserReference inputReference = mock(UserReference.class);
        XWikiDocument document = mock(XWikiDocument.class);
        when(this.configuration.isDelegateEnabled()).thenReturn(false);
        assertEquals(Collections.emptySet(),
            this.delegateApproverManager.getOriginalApprovers(inputReference, document));

        when(this.configuration.isDelegateEnabled()).thenReturn(true);

        String foo = "XWiki.Foo";
        String bar = "XWiki.Bar";
        String buz = "XWiki.Buz";

        UserReference fooRef = mock(UserReference.class);
        UserReference barRef = mock(UserReference.class);
        UserReference buzRef = mock(UserReference.class);

        when(this.userReferenceSerializer.serialize(fooRef)).thenReturn(foo);
        when(this.userReferenceSerializer.serialize(barRef)).thenReturn(bar);
        when(this.userReferenceSerializer.serialize(buzRef)).thenReturn(buz);

        when(this.approversManager.getAllApprovers(document, false))
            .thenReturn(new HashSet<>(List.of(fooRef, barRef, buzRef)));
        when(this.delegateCache.get(foo)).thenReturn(Collections.emptySet());
        when(this.delegateCache.get(bar)).thenReturn(Collections.singleton(inputReference));
        when(this.delegateCache.get(buz)).thenReturn(new HashSet<>(List.of(mock(UserReference.class), inputReference)));

        assertEquals(new HashSet<>(List.of(barRef, buzRef)),
            this.delegateApproverManager.getOriginalApprovers(inputReference, document));
    }
}
