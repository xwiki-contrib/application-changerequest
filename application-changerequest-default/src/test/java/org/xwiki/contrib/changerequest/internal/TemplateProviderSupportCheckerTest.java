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

import java.util.List;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheManager;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryExecutor;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link TemplateProviderSupportChecker}.
 *
 * @version $Id$
 * @since 0.9
 */
@ComponentTest
class TemplateProviderSupportCheckerTest
{
    @InjectMockComponents
    private TemplateProviderSupportChecker providerSupportChecker;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @MockComponent
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @MockComponent
    @Named("compactwiki")
    private EntityReferenceSerializer<String> compactWikiEntityReferenceSerializer;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    private QueryExecutor queryExecutor;

    @MockComponent
    @Named("unique")
    private QueryFilter uniqueQueryFilter;

    private Cache<Boolean> supportedProviderCache;
    private XWikiContext context;

    @BeforeComponent
    void initialize(MockitoComponentManager componentManager) throws Exception
    {
        CacheManager cacheManager = componentManager.registerMockComponent(CacheManager.class);
        this.supportedProviderCache = mock(Cache.class);
        when(cacheManager.createNewCache(any())).thenReturn((Cache) this.supportedProviderCache);
    }

    @BeforeEach
    void setup()
    {
        this.context = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(this.context);
    }

    @Test
    void isTemplateProviderSupportedWhenInCache() throws ChangeRequestException
    {
        DocumentReference templateProviderReference = mock(DocumentReference.class);
        String serializedReference = "serializedRef";
        when(this.entityReferenceSerializer.serialize(templateProviderReference)).thenReturn(serializedReference);
        when(this.supportedProviderCache.get(serializedReference)).thenReturn(true);
        assertTrue(this.providerSupportChecker.isTemplateProviderSupported(templateProviderReference));

        when(this.supportedProviderCache.get(serializedReference)).thenReturn(false);
        assertFalse(this.providerSupportChecker.isTemplateProviderSupported(templateProviderReference));
    }

    @Test
    void isTemplateProviderSupportedNoObject() throws Exception
    {
        DocumentReference templateProviderReference = mock(DocumentReference.class);
        String serializedReference = "serializedRef";
        when(this.entityReferenceSerializer.serialize(templateProviderReference)).thenReturn(serializedReference);
        when(this.supportedProviderCache.get(serializedReference)).thenReturn(null).thenReturn(false);
        XWiki xWiki = mock(XWiki.class);
        when(this.context.getWiki()).thenReturn(xWiki);

        XWikiDocument providerDoc = mock(XWikiDocument.class);
        when(xWiki.getDocument(templateProviderReference, context)).thenReturn(providerDoc);
        when(providerDoc.getXObject(TemplateProviderSupportChecker.TEMPLATE_PROVIDER_CLASS_REFERENCE)).thenReturn(null);
        assertFalse(this.providerSupportChecker.isTemplateProviderSupported(templateProviderReference));
        verify(this.supportedProviderCache).set(serializedReference, false);
    }

    @Test
    void isTemplateProviderSupportedNotEdit() throws Exception
    {
        DocumentReference templateProviderReference = mock(DocumentReference.class);
        String serializedReference = "serializedRef";
        when(this.entityReferenceSerializer.serialize(templateProviderReference)).thenReturn(serializedReference);
        when(this.supportedProviderCache.get(serializedReference)).thenReturn(null).thenReturn(false);
        XWiki xWiki = mock(XWiki.class);
        when(this.context.getWiki()).thenReturn(xWiki);

        XWikiDocument providerDoc = mock(XWikiDocument.class);
        when(xWiki.getDocument(templateProviderReference, context)).thenReturn(providerDoc);
        BaseObject templateObject = mock(BaseObject.class);
        when(providerDoc.getXObject(TemplateProviderSupportChecker.TEMPLATE_PROVIDER_CLASS_REFERENCE))
            .thenReturn(templateObject);
        when(templateObject.getStringValue("action")).thenReturn("saveandview");

        assertFalse(this.providerSupportChecker.isTemplateProviderSupported(templateProviderReference));
        verify(this.supportedProviderCache).set(serializedReference, false);
        verify(templateObject, never()).getStringValue("template");
    }

    @Test
    void isTemplateProviderSupportedTemplate() throws Exception
    {
        DocumentReference templateProviderReference = mock(DocumentReference.class);
        String serializedReference = "serializedRef";
        when(this.entityReferenceSerializer.serialize(templateProviderReference)).thenReturn(serializedReference);
        when(this.supportedProviderCache.get(serializedReference)).thenReturn(null).thenReturn(true);
        XWiki xWiki = mock(XWiki.class);
        when(this.context.getWiki()).thenReturn(xWiki);

        XWikiDocument providerDoc = mock(XWikiDocument.class);
        when(xWiki.getDocument(templateProviderReference, context)).thenReturn(providerDoc);
        BaseObject templateObject = mock(BaseObject.class);
        when(providerDoc.getXObject(TemplateProviderSupportChecker.TEMPLATE_PROVIDER_CLASS_REFERENCE))
            .thenReturn(templateObject);
        when(templateObject.getStringValue("action")).thenReturn("edit");

        DocumentReference templateReference = mock(DocumentReference.class);
        String serializedTemplateRef =  "templateRef";
        when(this.entityReferenceSerializer.serialize(templateReference)).thenReturn(serializedTemplateRef);
        when(this.supportedProviderCache.get(serializedTemplateRef)).thenReturn(true);
        when(this.documentReferenceResolver.resolve(serializedTemplateRef)).thenReturn(templateReference);
        when(templateObject.getStringValue("template")).thenReturn(serializedTemplateRef);

        assertTrue(this.providerSupportChecker.isTemplateProviderSupported(templateProviderReference));
        verify(this.supportedProviderCache).set(serializedReference, true);
        verify(this.supportedProviderCache, times(2)).get(serializedTemplateRef);
    }

    @Test
    void isTemplateSupportedWhenInCache() throws ChangeRequestException
    {
        DocumentReference templateReference = mock(DocumentReference.class);
        String serializedReference = "serializedTemplateRef";
        when(this.entityReferenceSerializer.serialize(templateReference)).thenReturn(serializedReference);
        when(this.supportedProviderCache.get(serializedReference)).thenReturn(true);
        assertTrue(this.providerSupportChecker.isTemplateSupported(templateReference));

        when(this.supportedProviderCache.get(serializedReference)).thenReturn(false);
        assertFalse(this.providerSupportChecker.isTemplateSupported(templateReference));
    }

    @Test
    void isTemplateSupportedWhenTerminal() throws ChangeRequestException
    {
        DocumentReference templateReference = new DocumentReference("xwiki", "Template", "Terminal");
        String serializedReference = "serializedTemplateRef";
        when(this.entityReferenceSerializer.serialize(templateReference)).thenReturn(serializedReference);
        when(this.supportedProviderCache.get(serializedReference)).thenReturn(null).thenReturn(true);

        assertTrue(this.providerSupportChecker.isTemplateSupported(templateReference));
        verify(this.supportedProviderCache).set(serializedReference, true);
    }

    @Test
    void isTemplateSupportedWhenHierarchy() throws Exception
    {
        DocumentReference templateReference = new DocumentReference("xwiki", "Template", "WebHome");
        String serializedReference = "serializedTemplateRef";
        when(this.entityReferenceSerializer.serialize(templateReference)).thenReturn(serializedReference);
        when(this.supportedProviderCache.get(serializedReference)).thenReturn(null).thenReturn(false);

        when(this.compactWikiEntityReferenceSerializer.serialize(templateReference.getLastSpaceReference()))
            .thenReturn("Template");
        String expectedQuery = "where doc.space like 'Template' or doc.space like 'Template.%'";
        Query query = mock(Query.class);
        when(this.queryManager.createQuery(expectedQuery, Query.XWQL)).thenReturn(query);
        List<String> results = mock(List.class);
        when(this.queryExecutor.execute(query)).thenReturn((List) results);
        when(results.size()).thenReturn(4);

        assertFalse(this.providerSupportChecker.isTemplateSupported(templateReference));
        verify(this.supportedProviderCache).set(serializedReference, false);
        verify(query).addFilter(this.uniqueQueryFilter);
        verify(query).setLimit(5);

        when(this.supportedProviderCache.get(serializedReference)).thenReturn(null).thenReturn(true);
        when(results.size()).thenReturn(1);
        assertTrue(this.providerSupportChecker.isTemplateSupported(templateReference));
        verify(this.supportedProviderCache).set(serializedReference, true);
    }
}
