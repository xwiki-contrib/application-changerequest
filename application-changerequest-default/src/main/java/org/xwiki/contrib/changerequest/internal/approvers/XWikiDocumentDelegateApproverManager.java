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
import java.util.stream.Collectors;

import javax.inject.Inject;
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
import org.xwiki.contrib.changerequest.ApproversManager;
import org.xwiki.contrib.changerequest.ChangeRequestConfiguration;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.DelegateApproverManager;
import org.xwiki.contrib.changerequest.internal.UserReferenceConverter;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.user.UserReferenceSerializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.mandatory.XWikiUsersDocumentInitializer;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseStringProperty;
import com.xpn.xwiki.objects.ListProperty;
import com.xpn.xwiki.objects.PropertyInterface;

/**
 * Default implementation of {@link DelegateApproverManager} for {@link XWikiDocument} entity.
 *
 * @version $Id$
 * @since 0.13
 */
@Component
@Singleton
public class XWikiDocumentDelegateApproverManager implements DelegateApproverManager<XWikiDocument>, Initializable,
    Disposable
{
    @Inject
    private ChangeRequestConfiguration configuration;

    @Inject
    private UserReferenceConverter userReferenceConverter;

    @Inject
    private UserReferenceResolver<String> stringUserReferenceResolver;

    @Inject
    private UserReferenceSerializer<String> userReferenceSerializer;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private Provider<ApproversManager<XWikiDocument>> approversManagerProvider;

    @Inject
    private CacheManager cacheManager;

    private Cache<Set<UserReference>> delegateCache;

    @Override
    public void initialize() throws InitializationException
    {
        try {
            this.delegateCache = this.cacheManager.createNewCache(new LRUCacheConfiguration("changerequest.delegate"));
        } catch (CacheException e) {
            throw new InitializationException("Error while initializing delegate cache", e);
        }
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        this.delegateCache.dispose();
    }

    private boolean hasDelegatePropertiesConfigured()
    {
        return !this.configuration.getDelegateClassPropertyList().isEmpty();
    }

    @Override
    public Set<UserReference> computeDelegates(UserReference userReference) throws ChangeRequestException
    {
        Set<UserReference> result = Collections.emptySet();
        if (configuration.isDelegateEnabled() && this.hasDelegatePropertiesConfigured()) {
            DocumentReference userDocReference = this.userReferenceConverter.convert(userReference);
            XWikiContext context = this.contextProvider.get();
            try {
                XWikiDocument userDoc = context.getWiki().getDocument(userDocReference, context);
                if (!userDoc.isNew()) {
                    result = this.getDelegatesFromProperties(userDoc);
                    userDoc.removeXObjects(DelegateApproversXClassInitializer.DELEGATE_APPROVERS_XCLASS);
                    int objectNumber =
                        userDoc.createXObject(DelegateApproversXClassInitializer.DELEGATE_APPROVERS_XCLASS, context);
                    BaseObject delegateObject =
                        userDoc.getXObject(DelegateApproversXClassInitializer.DELEGATE_APPROVERS_XCLASS, objectNumber);
                    List<String> serializedList = result.stream()
                        .map(this.userReferenceSerializer::serialize)
                        .collect(Collectors.toList());
                    delegateObject.setLargeStringValue(DelegateApproversXClassInitializer.DELEGATED_USERS_PROPERTY,
                        StringUtils.join(serializedList, ApproversXClassInitializer.SEPARATOR_CHARACTER));
                    context.getWiki().saveDocument(userDoc, "Computation of delegate approvers", context);
                }
            } catch (XWikiException e) {
                throw new ChangeRequestException(
                    String.format("Error when reading user document of [%s] to compute delegate", userReference), e);
            }
            this.delegateCache.set(this.userReferenceSerializer.serialize(userReference), result);
        }
        return result;
    }

    private Set<UserReference> getDelegatesFromProperties(XWikiDocument userDoc) throws ChangeRequestException
    {
        Set<UserReference> result = new HashSet<>();
        BaseObject xObject = userDoc.getXObject(XWikiUsersDocumentInitializer.XWIKI_USERS_DOCUMENT_REFERENCE);
        if (xObject != null) {
            for (String property : this.configuration.getDelegateClassPropertyList()) {
                try {
                    PropertyInterface propertyInterface = xObject.get(property);
                    if (propertyInterface != null) {
                        if (propertyInterface instanceof ListProperty) {
                            ListProperty values = (ListProperty) propertyInterface;
                            result.addAll(values.getList()
                                .stream()
                                .map(this.stringUserReferenceResolver::resolve)
                                .collect(Collectors.toSet()));
                        } else if (propertyInterface instanceof BaseStringProperty) {
                            BaseStringProperty values = (BaseStringProperty) propertyInterface;
                            result.addAll(Arrays.stream(StringUtils.split(values.getValue(),
                                    ApproversXClassInitializer.SEPARATOR_CHARACTER))
                                .map(this.stringUserReferenceResolver::resolve)
                                .collect(Collectors.toSet()));
                        }
                    }
                } catch (XWikiException e) {
                    throw new ChangeRequestException(
                        String.format("Error when reading property [%s] to compute delegate", property), e);
                }
            }
        }
        return result;
    }

    @Override
    public Set<UserReference> getDelegates(UserReference userReference) throws ChangeRequestException
    {
        Set<UserReference> result = Collections.emptySet();
        if (configuration.isDelegateEnabled()) {
            String serializedReference = this.userReferenceSerializer.serialize(userReference);
            result = this.delegateCache.get(serializedReference);
            if (result == null) {
                result = getDelegateWithoutCache(userReference);
                this.delegateCache.set(serializedReference, result);
            }
        }
        return result;
    }

    private Set<UserReference> getDelegateWithoutCache(UserReference userReference) throws ChangeRequestException
    {
        Set<UserReference> result = Collections.emptySet();
        DocumentReference userDocReference = this.userReferenceConverter.convert(userReference);
        XWikiContext context = this.contextProvider.get();
        try {
            XWikiDocument userDoc = context.getWiki().getDocument(userDocReference, context);
            BaseObject delegateObject =
                userDoc.getXObject(DelegateApproversXClassInitializer.DELEGATE_APPROVERS_XCLASS);
            if (delegateObject != null) {
                String value =
                    delegateObject.getLargeStringValue(
                        DelegateApproversXClassInitializer.DELEGATED_USERS_PROPERTY);
                if (!StringUtils.isEmpty(value)) {
                    result =
                        Arrays.stream(StringUtils.split(value, ApproversXClassInitializer.SEPARATOR_CHARACTER))
                            .map(this.stringUserReferenceResolver::resolve)
                            .collect(Collectors.toSet());
                }
            }
        } catch (XWikiException e) {
            throw new ChangeRequestException(
                String.format("Error when reading document of user [%s] to retrieve delegate", userDocReference), e);
        }
        return result;
    }

    @Override
    public boolean isDelegateApproverOf(UserReference userReference, XWikiDocument entity)
        throws ChangeRequestException
    {
        if (this.configuration.isDelegateEnabled()) {
            Set<UserReference> allApprovers = this.approversManagerProvider.get().getAllApprovers(entity, false);

            for (UserReference approver : allApprovers) {
                if (this.getDelegates(approver).contains(userReference)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isDelegateApproverOf(UserReference userReference, XWikiDocument entity,
        UserReference originalApprover) throws ChangeRequestException
    {
        boolean result = false;
        if (this.configuration.isDelegateEnabled()) {
            Set<UserReference> allApprovers = this.approversManagerProvider.get().getAllApprovers(entity, false);
            if (allApprovers.contains(originalApprover)) {
                result = this.getDelegates(originalApprover).contains(userReference);
            }
        }
        return result;
    }

    @Override
    public Set<UserReference> getOriginalApprovers(UserReference userReference, XWikiDocument entity)
        throws ChangeRequestException
    {
        Set<UserReference> result = new HashSet<>();
        if (this.configuration.isDelegateEnabled()) {
            Set<UserReference> allApprovers = this.approversManagerProvider.get().getAllApprovers(entity, false);

            for (UserReference approver : allApprovers) {
                if (this.getDelegates(approver).contains(userReference)) {
                    result.add(approver);
                }
            }
        }
        return result;
    }
}
