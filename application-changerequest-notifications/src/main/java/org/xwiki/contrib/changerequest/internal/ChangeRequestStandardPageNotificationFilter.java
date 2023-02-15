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

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequestConfiguration;
import org.xwiki.eventstream.Event;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.expression.ExpressionNode;
import org.xwiki.notifications.filters.internal.ToggleableNotificationFilter;
import org.xwiki.notifications.preferences.NotificationPreference;

import com.xpn.xwiki.XWikiContext;

import static java.util.Arrays.asList;

/**
 * Filter dedicated to ignore standard update / create / addComment events that might be triggered in change request
 * pages, since those are redundant with the change request events.
 *
 * @version $Id$
 * @since 1.4.5
 */
@Component
@Singleton
@Named(ChangeRequestStandardPageNotificationFilter.FILTER_NAME)
public class ChangeRequestStandardPageNotificationFilter implements NotificationFilter, ToggleableNotificationFilter
{
    /**
     * The name of the filter.
     */
    public static final String FILTER_NAME = "changeRequestStandardPageNotificationFilter";

    private static final List<String> XWIKI_EVENT_TYPES = asList("create", "update", "addComment");

    @Inject
    private Provider<ChangeRequestConfiguration> configurationProvider;

    @Inject
    private Provider<XWikiContext> contextProvider;

    private boolean isDocumentPartOfChangeRequestData(DocumentReference documentReference, WikiReference eventWiki)
    {
        boolean result = false;
        if (documentReference != null) {
            XWikiContext context = contextProvider.get();
            WikiReference currentWiki = context.getWikiReference();
            // We need to set the wiki reference to obtain the proper location
            context.setWikiReference(eventWiki);
            result = documentReference.hasParent(this.configurationProvider.get().getChangeRequestSpaceLocation());
            context.setWikiReference(currentWiki);
        }
        return result;
    }

    @Override
    public FilterPolicy filterEvent(Event event, DocumentReference user,
        Collection<NotificationFilterPreference> filterPreferences, NotificationFormat format)
    {
        if (XWIKI_EVENT_TYPES.contains(event.getType())
            && isDocumentPartOfChangeRequestData(event.getDocument(), event.getWiki())) {
            return FilterPolicy.FILTER;
        }
        return FilterPolicy.NO_EFFECT;
    }

    @Override
    public boolean matchesPreference(NotificationPreference preference)
    {
        return false;
    }

    @Override
    public ExpressionNode filterExpression(DocumentReference user,
        Collection<NotificationFilterPreference> filterPreferences, NotificationPreference preference)
    {
        return null;
    }

    @Override
    public ExpressionNode filterExpression(DocumentReference user,
        Collection<NotificationFilterPreference> filterPreferences, NotificationFilterType type,
        NotificationFormat format)
    {
        return null;
    }

    @Override
    public String getName()
    {
        return FILTER_NAME;
    }
}
