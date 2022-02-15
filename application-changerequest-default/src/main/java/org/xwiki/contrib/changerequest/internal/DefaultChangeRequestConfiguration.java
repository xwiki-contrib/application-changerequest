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
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.contrib.changerequest.ChangeRequestConfiguration;
import org.xwiki.contrib.changerequest.internal.strategies.AcceptAllMergeApprovalStrategy;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.SpaceReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.user.SuperAdminUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWikiContext;

/**
 * Default implementation of {@link ChangeRequestConfiguration}.
 *
 * @version $Id$
 * @since 0.1
 */
@Component
@Singleton
public class DefaultChangeRequestConfiguration implements ChangeRequestConfiguration
{
    static final String DEFAULT_APPROVAL_STRATEGY = AcceptAllMergeApprovalStrategy.NAME;
    private static final List<String> CHANGE_REQUEST_SPACE_LOCATION = Arrays.asList("ChangeRequest", "Data");

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    @Named("changerequest")
    private ConfigurationSource configurationSource;

    @Inject
    private SpaceReferenceResolver<String> spaceReferenceResolver;

    @Inject
    private UserReferenceResolver<String> userReferenceResolver;

    @Override
    public String getMergeApprovalStrategy()
    {
        return this.configurationSource.getProperty("approvalStrategy", DEFAULT_APPROVAL_STRATEGY);
    }

    @Override
    public SpaceReference getChangeRequestSpaceLocation()
    {
        WikiReference wikiReference = this.contextProvider.get().getWikiReference();
        String changeRequestLocation = this.configurationSource.getProperty("changeRequestLocation", null);
        SpaceReference result;
        if (changeRequestLocation != null) {
            result = this.spaceReferenceResolver.resolve(changeRequestLocation, wikiReference);
        } else {
            result = new SpaceReference(wikiReference.getName(), CHANGE_REQUEST_SPACE_LOCATION);
        }
        return result;
    }

    @Override
    public long getStaleChangeRequestDurationForClosing()
    {
        return this.configurationSource.getProperty("durationBeforeClosingStale", 5);
    }

    @Override
    public long getStaleChangeRequestDurationForNotifying()
    {
        return this.configurationSource.getProperty("durationBeforeNotifyingStale", 20);
    }

    @Override
    public boolean useCreationDateForStaleDurations()
    {
        return this.configurationSource.getProperty("useCreationDateForStaleDurations", false);
    }

    @Override
    public UserReference getSchedulerContextUser()
    {
        String schedulerContextUser = this.configurationSource.getProperty("schedulerContextUser");
        UserReference result;
        if (!StringUtils.isEmpty(schedulerContextUser)) {
            result = this.userReferenceResolver.resolve(schedulerContextUser);
        } else {
            result = SuperAdminUserReference.INSTANCE;
        }
        return result;
    }
}
