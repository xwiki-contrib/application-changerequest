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

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.contrib.changerequest.ChangeRequestConfiguration;
import org.xwiki.eventstream.Event;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ChangeRequestStandardPageNotificationFilter}.
 *
 * @version $Id$
 * @since 1.4.5
 */
@ComponentTest
public class ChangeRequestStandardPageNotificationFilterTest
{
    @InjectMockComponents
    private ChangeRequestStandardPageNotificationFilter notificationFilter;

    @MockComponent
    private ChangeRequestConfiguration configuration;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    private XWikiContext context;

    @BeforeEach
    void setup()
    {
        this.context = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(this.context);
    }

    @Test
    void filterEvent()
    {
        Event event = mock(Event.class);
        assertEquals(NotificationFilter.FilterPolicy.NO_EFFECT,
            this.notificationFilter.filterEvent(event, null, null, null));

        when(event.getType()).thenReturn("update");
        assertEquals(NotificationFilter.FilterPolicy.NO_EFFECT,
            this.notificationFilter.filterEvent(event, null, null, null));

        DocumentReference documentReference =
            new DocumentReference("subwiki", Arrays.asList("ChangeRequest", "Data", "MyCR"), "WebHome");
        when(event.getDocument()).thenReturn(documentReference);

        WikiReference currentWiki = new WikiReference("foo");
        when(context.getWikiReference()).thenReturn(currentWiki);
        WikiReference eventWiki = new WikiReference("subwiki");
        when(event.getWiki()).thenReturn(eventWiki);

        when(this.configuration.getChangeRequestSpaceLocation()).thenReturn(
            new SpaceReference(eventWiki.getName(), Arrays.asList("ChangeRequest", "Data")));
        assertEquals(NotificationFilter.FilterPolicy.FILTER,
            this.notificationFilter.filterEvent(event, null, null, null));
        verify(this.context).setWikiReference(eventWiki);
        verify(this.context).setWikiReference(currentWiki);

        when(event.getType()).thenReturn("changerequest.create");
        assertEquals(NotificationFilter.FilterPolicy.NO_EFFECT,
            this.notificationFilter.filterEvent(event, null, null, null));

        when(event.getType()).thenReturn("create");
        when(this.configuration.getChangeRequestSpaceLocation()).thenReturn(
            new SpaceReference(eventWiki.getName(), Arrays.asList("Foo", "Data")));
        assertEquals(NotificationFilter.FilterPolicy.NO_EFFECT,
            this.notificationFilter.filterEvent(event, null, null, null));

        when(this.configuration.getChangeRequestSpaceLocation()).thenReturn(
            new SpaceReference(eventWiki.getName(), Arrays.asList("ChangeRequest")));
        assertEquals(NotificationFilter.FilterPolicy.FILTER,
            this.notificationFilter.filterEvent(event, null, null, null));

        when(this.configuration.getChangeRequestSpaceLocation()).thenReturn(
            new SpaceReference(eventWiki.getName(), Arrays.asList("ChangeRequest", "Data")));
        documentReference =
            new DocumentReference("subwiki", Arrays.asList("ChangeRequest", "SomePage"), "WebHome");
        when(event.getDocument()).thenReturn(documentReference);
        assertEquals(NotificationFilter.FilterPolicy.NO_EFFECT,
            this.notificationFilter.filterEvent(event, null, null, null));
    }
}
