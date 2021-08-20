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
package org.xwiki.contrib.changerequest.internal.strategies;

import javax.inject.Inject;

import org.xwiki.contrib.changerequest.MergeApprovalStrategy;
import org.xwiki.localization.ContextualLocalizationManager;

/**
 * Abstract component for approval strategies.
 *
 * @version $Id$
 * @since 0.4
 */
public abstract class AbstractMergeApprovalStrategy implements MergeApprovalStrategy
{
    private static final String TRANSLATION_PREFIX = "changerequest.strategies.";

    @Inject
    protected ContextualLocalizationManager contextualLocalizationManager;

    private final String name;

    /**
     * Default constructor.
     *
     * @param name the name of the strategy, which should also be used as hint.
     */
    public AbstractMergeApprovalStrategy(String name)
    {
        this.name = name;
    }

    /**
     * @return a translation prefix based on the name of the strategy.
     */
    protected String getTranslationPrefix()
    {
        return TRANSLATION_PREFIX + getName() + ".";
    }

    @Override
    public String getName()
    {
        return this.name;
    }

    @Override
    public String getDescription()
    {
        return this.contextualLocalizationManager.getTranslationPlain(getTranslationPrefix() + "description");
    }
}
