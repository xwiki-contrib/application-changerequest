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
package org.xwiki.contrib.changerequest.rights;

import java.util.Collections;
import java.util.Set;

import org.xwiki.model.EntityType;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.RightDescription;
import org.xwiki.security.authorization.RuleState;
import org.xwiki.stability.Unstable;

/**
 * Rights for people who are able to mark a change request as approved or needing changes.
 * Only  the people with approver rights are able to merge a change request.
 *
 * @version $Id$
 * @since 0.1-SNAPSHOT
 */
@Unstable
public final class ChangeRequestApproveRight implements RightDescription
{
    /**
     * Singleton instance of the right.
     */
    public static final ChangeRequestApproveRight INSTANCE = new ChangeRequestApproveRight();

    private ChangeRequestApproveRight()
    {
    }

    @Override
    public String getName()
    {
        return "crapprove";
    }

    @Override
    public RuleState getDefaultState()
    {
        return RuleState.DENY;
    }

    @Override
    public RuleState getTieResolutionPolicy()
    {
        return RuleState.DENY;
    }

    @Override
    public boolean getInheritanceOverridePolicy()
    {
        return false;
    }

    @Override
    public Set<Right> getImpliedRights()
    {
        return Collections.singleton(Right.EDIT);
    }

    @Override
    public Set<EntityType> getTargetedEntityType()
    {
        return Collections.singleton(EntityType.DOCUMENT);
    }

    @Override
    public boolean isReadOnly()
    {
        return false;
    }
}
