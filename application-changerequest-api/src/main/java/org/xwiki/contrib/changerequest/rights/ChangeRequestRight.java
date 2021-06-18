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
 * Default right for people who should be able to create change requests.
 *
 * @version $Id$
 * @since 0.1
 */
@Unstable
public final class ChangeRequestRight implements RightDescription
{
    /**
     * Actual singleton instance of the right.
     */
    public static final ChangeRequestRight INSTANCE = new ChangeRequestRight();

    private ChangeRequestRight()
    {
    }

    @Override
    public String getName()
    {
        return "changerequest";
    }

    @Override
    public RuleState getDefaultState()
    {
        return RuleState.ALLOW;
    }

    @Override
    public RuleState getTieResolutionPolicy()
    {
        return RuleState.DENY;
    }

    @Override
    public boolean getInheritanceOverridePolicy()
    {
        return true;
    }

    @Override
    public Set<Right> getImpliedRights()
    {
        return Collections.emptySet();
    }

    @Override
    public Set<EntityType> getTargetedEntityType()
    {
        return Right.WIKI_SPACE_DOCUMENT;
    }

    @Override
    public boolean isReadOnly()
    {
        return false;
    }

    /**
     * Retrieve the actual right corresponding to this right description.
     * Note that this method can only be called after the right has been registered.
     *
     * @return the right corresponding to this description.
     */
    public static Right getRight()
    {
        return Right.toRight(INSTANCE.getName());
    }
}
