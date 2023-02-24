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
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequestConfiguration;
import org.xwiki.contrib.changerequest.internal.ChangeRequestConfigurationSource;
import org.xwiki.contrib.changerequest.internal.DefaultChangeRequestConfiguration;
import org.xwiki.contrib.changerequest.internal.cache.DiffCacheManager;
import org.xwiki.contrib.changerequest.internal.jobs.DelegateApproversComputationRequest;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.RegexEntityReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.event.AbstractLocalEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XObjectUpdatedEvent;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;

/**
 * Listener in charge of triggering a global computation of delegate approvers if the configuration change to enable
 * the mechanism, or to update the list of fields to take into account in XWikiUser.
 * This listener also invalidate the {@link DiffCacheManager} as a configuration change might mean an update in the
 * {@link ChangeRequestConfiguration#getRenderedDiffComponent()}.
 *
 * @version $Id$
 * @since 0.13
 */
@Component
@Singleton
@Named(ChangeRequestConfigurationUpdatedListener.NAME)
public class ChangeRequestConfigurationUpdatedListener extends AbstractLocalEventListener
{
    static final String NAME =
        "org.xwiki.contrib.changerequest.internal.listeners.ChangeRequestConfigurationUpdatedListener";

    private static final RegexEntityReference REFERENCE =
        BaseObjectReference.any(ChangeRequestConfigurationSource.CLASS_REFERENCE.toString());

    private static final List<Event> EVENT_LIST = Collections.singletonList(
        new XObjectUpdatedEvent(REFERENCE)
    );

    @Inject
    private Provider<QueryManager> queryManagerProvider;

    @Inject
    private DocumentReferenceResolver<String> stringDocumentReferenceResolver;

    @Inject
    private Provider<JobExecutor> jobExecutorProvider;

    @Inject
    private Provider<DiffCacheManager> diffCacheManagerProvider;

    @Inject
    private Logger logger;

    /**
     * Default constructor.
     */
    public ChangeRequestConfigurationUpdatedListener()
    {
        super(NAME, EVENT_LIST);
    }

    @Override
    public void processLocalEvent(Event event, Object source, Object data)
    {
        XWikiDocument configurationDoc = (XWikiDocument) source;
        if (configurationDoc.getDocumentReference().getLocalDocumentReference()
            .equals(ChangeRequestConfigurationSource.DOC_REFERENCE)) {
            XWikiDocument originalConfigurationDoc = configurationDoc.getOriginalDocument();
            BaseObject currentObj = configurationDoc.getXObject(ChangeRequestConfigurationSource.CLASS_REFERENCE);
            BaseObject previousObj =
                originalConfigurationDoc.getXObject(ChangeRequestConfigurationSource.CLASS_REFERENCE);

            if (this.shouldRecomputeDelegate(currentObj, previousObj)) {
                List<EntityReference> allUsers =
                    this.getAllUsers(configurationDoc.getDocumentReference().getWikiReference());
                this.startComputationJob(allUsers);
            }
        }
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        super.onEvent(event, source, data);

        // We always invalidate the diff manager cache, even in case of remote event.
        XWikiDocument configurationDoc = (XWikiDocument) source;
        if (configurationDoc.getDocumentReference().getLocalDocumentReference()
            .equals(ChangeRequestConfigurationSource.DOC_REFERENCE)) {
            this.diffCacheManagerProvider.get().invalidateAll();
        }
    }

    private void startComputationJob(List<EntityReference> userList)
    {
        if (!userList.isEmpty()) {
            DelegateApproversComputationRequest computationRequest = new DelegateApproversComputationRequest();
            computationRequest.setEntityReferences(userList);
            computationRequest.setDeep(false);
            computationRequest.setInteractive(false);
            try {
                this.jobExecutorProvider.get()
                    .execute(DelegateApproversComputationRequest.DELEGATE_APPROVERS_COMPUTATION_JOB,
                        computationRequest);
            } catch (JobException e) {
                logger.error("Error when executing the computation job for delegates", e);
            }
        }
    }

    // FIXME: might not be good for subwikis depending on the members config
    private List<EntityReference> getAllUsers(WikiReference wikiReference)
    {
        List<EntityReference> result = new ArrayList<>();
        String statement = "select distinct doc.fullName from Document doc, "
            + "doc.object(XWiki.XWikiUsers) objUser where objUser.active = 1 order by doc.fullName";
        try {
            Query query = this.queryManagerProvider.get().createQuery(statement, Query.XWQL);
            query.setWiki(wikiReference.getName());
            List<String> userList = query.execute();
            for (String userName : userList) {
                result.add(this.stringDocumentReferenceResolver.resolve(userName));
            }
        } catch (QueryException e) {
            this.logger.error("Error while loading the list of users", e);
        }
        return result;
    }

    private boolean shouldRecomputeDelegate(BaseObject currentObj, BaseObject previousObj)
    {
        boolean result = false;
        if (currentObj != null && previousObj == null) {
            result = true;
        } else if (currentObj != null) {
            String currentDelegatePropertyValue =
                currentObj.getStringValue(DefaultChangeRequestConfiguration.DELEGATE_CLASS_PROPERTY_LIST_PROPERTY);
            String oldDelegatePropertyValue =
                previousObj.getStringValue(DefaultChangeRequestConfiguration.DELEGATE_CLASS_PROPERTY_LIST_PROPERTY);

            int currentDelegateEnabledValue =
                currentObj.getIntValue(DefaultChangeRequestConfiguration.DELEGATE_ENABLED_PROPERTY);
            int oldDelegateEnabledValue =
                previousObj.getIntValue(DefaultChangeRequestConfiguration.DELEGATE_ENABLED_PROPERTY);
            result = (currentDelegateEnabledValue == 1 && !StringUtils.isEmpty(currentDelegatePropertyValue))
                && (currentDelegateEnabledValue != oldDelegateEnabledValue
                || !StringUtils.equals(currentDelegatePropertyValue, oldDelegatePropertyValue));
        }

        return result;
    }
}
