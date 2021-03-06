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
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.contrib.changerequest.MergeApprovalStrategy;

/**
 * This strategy checks that all explicit approvers have approved the given change request.
 * If the change request does not have an explicit list of approvers, then the strategy fallbacks on
 * {@link OnlyApprovedMergeApprovalStrategy}.
 *
 * @version $Id$
 * @since 0.5
 */
@Component
@Singleton
@Named(AllApproversMergeApprovalStrategy.NAME)
public class AllApproversMergeApprovalStrategy extends AbstractAllApproversMergeApprovalStrategy
    implements Initializable
{
    static final String NAME = "allapprovers";

    @Inject
    @Named(OnlyApprovedMergeApprovalStrategy.NAME)
    private MergeApprovalStrategy onlyApprovedFallbackStrategy;

    /**
     * Default constructor.
     */
    public AllApproversMergeApprovalStrategy()
    {
        super(NAME);
    }

    @Override
    public void initialize() throws InitializationException
    {
        this.setFallbackStrategy(this.onlyApprovedFallbackStrategy);
    }
}
