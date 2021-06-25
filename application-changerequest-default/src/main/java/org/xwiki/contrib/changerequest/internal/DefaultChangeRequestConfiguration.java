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
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.ChangeRequestConfiguration;
import org.xwiki.contrib.changerequest.internal.strategies.AcceptAllMergeApprovalStrategy;
import org.xwiki.model.reference.SpaceReference;

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
    private static final List<String> CHANGE_REQUEST_SPACE_LOCATION = Arrays.asList("XWiki", "ChangeRequest");

    @Inject
    private Provider<XWikiContext> contextProvider;

    // FIXME: This needs to be replaced by a proper configuration from a doc.
    @Override
    public String getMergeApprovalStrategy()
    {
        return AcceptAllMergeApprovalStrategy.NAME;
    }

    @Override
    public SpaceReference getChangeRequestSpaceLocation()
    {
        return new SpaceReference(this.contextProvider.get().getWikiReference().getName(),
            CHANGE_REQUEST_SPACE_LOCATION);
    }
}
