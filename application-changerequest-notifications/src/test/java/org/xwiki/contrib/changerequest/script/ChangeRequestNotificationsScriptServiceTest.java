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

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.eventstream.EventSearchResult;
import org.xwiki.eventstream.EventStore;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.eventstream.query.SimpleEventQuery;
import org.xwiki.eventstream.query.SortableEventQuery;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ChangeRequestNotificationsScriptService}.
 *
 * @version $Id$
 * @since 0.11
 */
@ComponentTest
class ChangeRequestNotificationsScriptServiceTest
{
    @InjectMockComponents
    private ChangeRequestNotificationsScriptService scriptService;

    @MockComponent
    private Provider<EventStore> eventStoreProvider;

    private EventStore eventStore;

    @BeforeEach
    void setup()
    {
        this.eventStore = mock(EventStore.class);
        when(this.eventStoreProvider.get()).thenReturn(this.eventStore);
    }

    @Test
    void getChangeRequestEvents() throws EventStreamException
    {
        String crId = "crId42";
        int offset = 13;
        int limit = 42;
        SimpleEventQuery simpleEventQuery = new SimpleEventQuery(offset, limit)
            .eq("changerequest.id__properties_string", crId)
            .addSort("date", SortableEventQuery.SortClause.Order.ASC);
        EventSearchResult eventSearchResult = mock(EventSearchResult.class);
        when(this.eventStore.search(simpleEventQuery)).thenReturn(eventSearchResult);

        assertSame(eventSearchResult, this.scriptService.getChangeRequestEvents(crId, offset, limit));
    }
}
