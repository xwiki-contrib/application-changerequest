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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.LRUCacheConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryExecutor;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Dedicated component to check if a template provider is supported in change request.
 * Right now we don't support template providers using something else than edit action, and with a template having a
 * hierarchy of pages.
 *
 * @version $Id$
 * @since 0.9
 */
@Component(roles = TemplateProviderSupportChecker.class)
@Singleton
public class TemplateProviderSupportChecker implements Initializable, Disposable
{
    protected static final LocalDocumentReference TEMPLATE_PROVIDER_CLASS_REFERENCE =
        new LocalDocumentReference("XWiki", "TemplateProviderClass");

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> compactWikiEntityReferenceSerializer;

    @Inject
    private QueryManager queryManager;

    @Inject
    private QueryExecutor queryExecutor;

    @Inject
    @Named("unique")
    private QueryFilter uniqueQueryFilter;

    @Inject
    private CacheManager cacheManager;

    private Cache<Boolean> supportedProviderCache;

    @Override
    public void initialize() throws InitializationException
    {
        try {
            this.supportedProviderCache =
                this.cacheManager.createNewCache(new LRUCacheConfiguration("changerequest.templateproviders", 100));
        } catch (CacheException e) {
            throw new InitializationException("Error when initializing cache of supported template providers", e);
        }
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        this.supportedProviderCache.dispose();
    }

    /**
     * Check if the given template provider is currently supported by change request in page creation.
     *
     * @param templateProviderReference reference of the template provider to check for support.
     * @return {@code true} if the given template provider is supported.
     * @throws ChangeRequestException in case of problem for loading information.
     */
    public boolean isTemplateProviderSupported(DocumentReference templateProviderReference)
        throws ChangeRequestException
    {
        String cacheKey = this.entityReferenceSerializer.serialize(templateProviderReference);
        if (this.supportedProviderCache.get(cacheKey) == null) {
            boolean isSupported = this.computeTemplateProviderSupported(templateProviderReference);
            this.supportedProviderCache.set(cacheKey, isSupported);
        }

        return this.supportedProviderCache.get(cacheKey);
    }

    /**
     * Check if the given template is currently supported by change request in page creation.
     *
     * @param templateReference reference of the template to check for support.
     * @return {@code true} if the given template is supported.
     * @throws ChangeRequestException in case of problem for loading information.
     */
    public boolean isTemplateSupported(DocumentReference templateReference)
        throws ChangeRequestException
    {
        String cacheKey = this.entityReferenceSerializer.serialize(templateReference);
        if (this.supportedProviderCache.get(cacheKey) == null) {
            boolean isSupported = this.computeTemplateSupported(templateReference);
            this.supportedProviderCache.set(cacheKey, isSupported);
        }

        return this.supportedProviderCache.get(cacheKey);
    }

    private boolean computeTemplateProviderSupported(DocumentReference templateProviderReference)
        throws ChangeRequestException
    {
        boolean isSupported = false;
        XWikiContext context = this.contextProvider.get();
        try {
            XWikiDocument document = context.getWiki().getDocument(templateProviderReference, context);
            BaseObject templateObject = document.getXObject(TEMPLATE_PROVIDER_CLASS_REFERENCE);
            if (templateObject != null) {
                // we only support template providers with edit action.
                if (StringUtils.equals("edit", templateObject.getStringValue("action"))) {
                    String template = templateObject.getStringValue("template");
                    DocumentReference templateReference = this.documentReferenceResolver.resolve(template);
                    isSupported = this.isTemplateSupported(templateReference);
                }
            }
        } catch (XWikiException e) {
            throw new ChangeRequestException(
                String.format("Error when loading template provider [%s]", templateProviderReference), e);
        }
        return isSupported;
    }

    private boolean computeTemplateSupported(DocumentReference templateReference)
        throws ChangeRequestException
    {
        boolean result;
        if (!StringUtils.equals(XWiki.DEFAULT_SPACE_HOMEPAGE, templateReference.getName())) {
            result = true;
        } else {
            SpaceReference templateSpace = templateReference.getLastSpaceReference();
            String statement = String.format("where doc.space like '%1$s' or doc.space like '%1$s.%%'",
                this.compactWikiEntityReferenceSerializer.serialize(templateSpace));
            try {
                Query query = this.queryManager.createQuery(statement, Query.XWQL);
                query.setLimit(5);
                query.addFilter(uniqueQueryFilter);
                List<String> results = this.queryExecutor.execute(query);
                result = results.size() < 2;
            } catch (QueryException e) {
                throw new ChangeRequestException(
                    String.format("Error when creating query to list nested documents of [%s]", templateSpace), e);
            }
        }
        return result;
    }
}
