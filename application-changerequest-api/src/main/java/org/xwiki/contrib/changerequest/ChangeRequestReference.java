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
package org.xwiki.contrib.changerequest;

import org.xwiki.resource.AbstractResourceReference;
import org.xwiki.resource.ResourceType;
import org.xwiki.stability.Unstable;

/**
 * Represents a reference to a change request.
 * Such reference is represented by an action to perform on the change request, and the ID of the change request.
 *
 * @version $Id$
 * @since 0.1
 */
@Unstable
public class ChangeRequestReference extends AbstractResourceReference
{
    /**
     * The default resource type for those references.
     */
    public static final ResourceType TYPE = new ResourceType("changerequest");

    /**
     * The various actions available on the change requests.
     *
     * @version $Id$
     * @since 0.1
     */
    public enum ChangeRequestAction
    {
        /**
         * For creating a new change request.
         * Note that for this action, the id of the reference should be null, since the id is created later.
         */
        CREATE
    }

    private ChangeRequestAction action;
    private String id;

    /**
     * Default constructor of the reference.
     *
     * @param action the action to perform.
     * @param id the identifier of the change request, or null if it's a {@link ChangeRequestAction#CREATE}.
     */
    public ChangeRequestReference(ChangeRequestAction action, String id)
    {
        setType(TYPE);
        this.action = action;
        this.id = id;
    }

    /**
     * @return the action of the reference.
     */
    public ChangeRequestAction getAction()
    {
        return action;
    }

    /**
     * @return the identifier of the change request.
     */
    public String getId()
    {
        return id;
    }
}
