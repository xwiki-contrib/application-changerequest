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

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

/**
 * Same strategy component as {@link AllApproversMergeApprovalStrategy} except that this one doesn't provide any
 * fallback if the list of approvers is empty.
 *
 * @version $Id$
 * @since 0.8
 */
@Component
@Named(AllApproversNoFallbackMergeApprovalStrategy.NAME)
@Singleton
public class AllApproversNoFallbackMergeApprovalStrategy extends AbstractAllApproversMergeApprovalStrategy
{
    static final String NAME = "allApproversNoFallback";

    /**
     * Default constructor.
     */
    public AllApproversNoFallbackMergeApprovalStrategy()
    {
        super(NAME);
    }
}
