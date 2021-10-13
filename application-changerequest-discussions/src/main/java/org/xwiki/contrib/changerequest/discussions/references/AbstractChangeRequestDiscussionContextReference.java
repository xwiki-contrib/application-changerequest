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
package org.xwiki.contrib.changerequest.discussions.references;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.xwiki.stability.Unstable;

/**
 * Define references that are used for attaching discussions to change request UI elements.
 * Each reference instance allows to create a specific
 * {@link org.xwiki.contrib.discussions.domain.references.DiscussionContextEntityReference} which is itself contained in
 * a {@link org.xwiki.contrib.discussions.domain.DiscussionContext} linked to the discussion created.
 * We consider that there is a hierarchy of references, from the most abstract (i.e. the change request reference) to
 * the most concrete (e.g. a specific review reference). This allows to retrieve all references of a change request,
 * and to also attach a reference to a specific review or diff line to properly display the discussions.
 *
 * @version $Id$
 * @since 0.6
 */
@Unstable
public abstract class AbstractChangeRequestDiscussionContextReference
{
    private final String changeRequestId;
    private final ChangeRequestDiscussionReferenceType type;
    private final String reference;

    /**
     * Default constructor.
     *
     * @param changeRequestId the id of the change request related to this reference
     * @param type the type of reference
     * @param reference the actual specific reference element: this value is specific depending on the type of the
     *                  reference
     */
    protected AbstractChangeRequestDiscussionContextReference(String changeRequestId,
        ChangeRequestDiscussionReferenceType type, String reference)
    {
        this.changeRequestId = changeRequestId;
        this.type = type;
        this.reference = reference;
    }

    /**
     * @return the identifier of the change request related to the reference.
     */
    public String getChangeRequestId()
    {
        return changeRequestId;
    }

    /**
     * @return the type of the reference.
     */
    public ChangeRequestDiscussionReferenceType getType()
    {
        return type;
    }

    /**
     * @return the specific reference element: this value meaning depends on the type.
     */
    public String getReference()
    {
        return reference;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AbstractChangeRequestDiscussionContextReference that = (AbstractChangeRequestDiscussionContextReference) o;

        return new EqualsBuilder().append(changeRequestId, that.changeRequestId)
            .append(type, that.type).append(reference, that.reference).isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37).append(changeRequestId).append(type).append(reference).toHashCode();
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this)
            .append("changeRequestId", changeRequestId)
            .append("type", type)
            .append("reference", reference)
            .toString();
    }
}
