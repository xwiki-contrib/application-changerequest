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
package org.xwiki.contrib.changerequest.script;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestConfiguration;
import org.xwiki.contrib.changerequest.ChangeRequestManager;
import org.xwiki.contrib.changerequest.MergeApprovalStrategy;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;

/**
 * Script service for change request.
 *
 * @version $Id$
 * @since 0.1-SNAPSHOT
 */
@Unstable
@Component
@Named("changerequest")
@Singleton
public class ChangeRequestScriptService implements ScriptService
{
    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Inject
    private ChangeRequestConfiguration configuration;

    @Inject
    private ChangeRequestManager changeRequestManager;

    @Inject
    private Logger logger;

    private MergeApprovalStrategy getMergeApprovalStrategy() throws ComponentLookupException
    {
        return this.componentManager.getInstance(MergeApprovalStrategy.class,
           this.configuration.getMergeApprovalStrategy());
    }

    /**
     * Retrieve the change request identified with the given id and check if it can be merged.
     *
     * @param changeRequestId the identifier of the change request to check.
     * @return {@code true} if a change request matching the id is found and can be merged,
     *         {@code false} in all other cases.
     */
    public boolean canBeMerged(String changeRequestId)
    {
        Optional<ChangeRequest> changeRequest = this.changeRequestManager.getChangeRequest(changeRequestId);
        boolean result = false;
        if (changeRequest.isPresent()) {
            try {
                MergeApprovalStrategy mergeApprovalStrategy = getMergeApprovalStrategy();
                result = mergeApprovalStrategy.canBeMerged(changeRequest.get());
            } catch (ComponentLookupException e) {
                this.logger.warn("Error when getting the merge approval strategy: [{}]",
                    ExceptionUtils.getRootCauseMessage(e));
            }
        } else {
            this.logger.warn("Cannot find change request with id [{}].", changeRequestId);
        }
        return result;
    }
}
