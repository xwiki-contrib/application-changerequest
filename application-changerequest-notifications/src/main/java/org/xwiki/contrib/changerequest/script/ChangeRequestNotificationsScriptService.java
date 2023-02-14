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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.internal.ChangeRequestTitleCacheManager;
import org.xwiki.eventstream.EventSearchResult;
import org.xwiki.eventstream.EventStore;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.eventstream.query.SimpleEventQuery;
import org.xwiki.eventstream.query.SortableEventQuery;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;

import static org.xwiki.contrib.changerequest.internal.converters.AbstractChangeRequestRecordableEventConverter.CHANGE_REQUEST_ID_PARAMETER_KEY;

/**
 * Script service related to notifications on change request.
 *
 * @version $Id$
 * @since 0.6
 */
@Component
@Named("changerequest.notifications")
@Singleton
@Unstable
public class ChangeRequestNotificationsScriptService implements ScriptService
{
    @Inject
    private Provider<EventStore> eventStoreProvider;

    @Inject
    private Provider<ChangeRequestTitleCacheManager> titleCacheManagerProvider;

    /**
     * Search for events related to the given change request.
     *
     * @param changeRequestId the identifier of the change request for which to retrieve events.
     * @param limit the number of events to retrieve.
     * @param offset the offset where to start retrieving the events.
     * @return the events matching the given parameters ordered by date.
     * @throws EventStreamException in case of problem for searching the events.
     */
    public EventSearchResult getChangeRequestEvents(String changeRequestId, int offset, int limit)
        throws EventStreamException
    {
        // FIXME: this can be simplified with XWiki 13.9 API
        SimpleEventQuery eventQuery = new SimpleEventQuery(offset, limit)
            .eq(String.format("%s__properties_string",
                CHANGE_REQUEST_ID_PARAMETER_KEY), changeRequestId)
            .addSort("date", SortableEventQuery.SortClause.Order.ASC);

        return this.eventStoreProvider.get().search(eventQuery);
    }

    /**
     * Retrieve the document title to be displayed for the given change request and file change identifier.
     * This method automatically compute and cache the title to be displayed for the filechange identified by the given
     * information.
     *
     * @param changeRequestId the identifier of a change request
     * @param fileChangeId the identifier of a filechange
     * @return a computed title of a page to be displayed or {@code null}
     */
    public String getPageTitle(String changeRequestId, String fileChangeId)
    {
        return this.titleCacheManagerProvider.get().getTitle(changeRequestId, fileChangeId);
    }
}
