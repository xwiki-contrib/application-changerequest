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
package org.xwiki.contrib.changerequest.internal.jobs;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestConfiguration;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestManager;
import org.xwiki.contrib.changerequest.ChangeRequestStatus;
import org.xwiki.contrib.changerequest.events.StaleChangeRequestEvent;
import org.xwiki.contrib.changerequest.internal.UserReferenceConverter;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.user.UserReference;

import com.xpn.xwiki.XWikiContext;

/**
 * Component in charge of the execution of the different jobs.
 * @see CloseStaleChangeRequestsJob
 * @see NotifyStaleChangeRequestsJob
 *
 * @version $Id$
 * @since 0.10
 */
@Component(roles = ChangeRequestSchedulerJobManager.class)
@Singleton
public class ChangeRequestSchedulerJobManager
{
    @Inject
    private ChangeRequestConfiguration configuration;

    @Inject
    private ChangeRequestStorageManager changeRequestStorageManager;

    @Inject
    private ChangeRequestManager changeRequestManager;

    @Inject
    private ObservationManager observationManager;

    @Inject
    private UserReferenceConverter userReferenceConverter;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private Logger logger;

    /**
     * Automatically close the stale change requests when needed.
     */
    public void closeStaleChangeRequests()
    {
        long durationForClosing = this.configuration.getStaleChangeRequestDurationForClosing();
        long durationForNotifying = this.configuration.getStaleChangeRequestDurationForNotifying();

        if (durationForClosing > 0) {
            try {
                List<ChangeRequest> changeRequests;
                if (durationForNotifying > 0) {
                    Date limitDate = getLimitDate(durationForClosing);
                    changeRequests = this.changeRequestStorageManager.findChangeRequestsStaledBefore(limitDate);
                } else {
                    changeRequests = this.retrieveStaleChangeRequests(durationForClosing);
                }

                if (!changeRequests.isEmpty()) {
                    this.setContextUser();
                    for (ChangeRequest changeRequest : changeRequests) {
                        this.changeRequestManager.updateStatus(changeRequest, ChangeRequestStatus.STALE);
                    }
                }
            } catch (ChangeRequestException e) {
                this.logger.error("Error while trying to close stale change requests.", e);
            }
        }
    }

    /**
     * Automatically trigger a {@link StaleChangeRequestEvent} for the concerned change requests.
     */
    public void notifyStaleChangeRequests()
    {
        long durationLimit = this.configuration.getStaleChangeRequestDurationForNotifying();
        List<ChangeRequest> changeRequests = null;
        try {
            changeRequests = this.retrieveStaleChangeRequests(durationLimit);
        } catch (ChangeRequestException e) {
            this.logger.error("Error while retrieving stale change requests.", e);
            changeRequests = Collections.emptyList();
        }

        if (!changeRequests.isEmpty()) {
            this.setContextUser();
            for (ChangeRequest changeRequest : changeRequests) {
                this.handleChangeRequestNotification(changeRequest);
            }
        }
    }

    private void setContextUser()
    {
        UserReference schedulerContextUser = this.configuration.getSchedulerContextUser();
        DocumentReference schedulerUser = this.userReferenceConverter.convert(schedulerContextUser);
        XWikiContext context = this.contextProvider.get();
        context.setUserReference(schedulerUser);
    }

    private void handleChangeRequestNotification(ChangeRequest changeRequest)
    {
        if (changeRequest.getStaleDate() == null) {
            this.observationManager.notify(new StaleChangeRequestEvent(), changeRequest.getId(), changeRequest);
            changeRequest.setStaleDate(new Date());
            try {
                this.changeRequestStorageManager.saveStaleDate(changeRequest);
            } catch (ChangeRequestException e) {
                this.logger.error("Error while saving the change request stale date", e);
            }
        }
    }

    private Date getLimitDate(long durationLimit)
    {
        Date now = new Date();
        return Date.from(now.toInstant().minus(durationLimit, this.configuration.getDurationUnit()));
    }

    private List<ChangeRequest> retrieveStaleChangeRequests(long durationLimit) throws ChangeRequestException
    {
        if (durationLimit > 0) {
            boolean useCreationDate = this.configuration.useCreationDateForStaleDurations();
            return this.changeRequestStorageManager
                .findOpenChangeRequestsByDate(getLimitDate(durationLimit), useCreationDate);
        } else {
            return Collections.emptyList();
        }
    }
}
