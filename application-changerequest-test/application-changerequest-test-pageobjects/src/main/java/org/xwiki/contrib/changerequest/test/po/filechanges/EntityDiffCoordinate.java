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
package org.xwiki.contrib.changerequest.test.po.filechanges;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Allow to specify coordinates inside a {@link ChangeRequestEntityDiff}.
 *
 * @version $Id$
 * @since 1.12
 */
public class EntityDiffCoordinate
{
    private final String property;
    private final LineChange lineChange;
    private final int lineNumber;

    /**
     * Default constructor.
     *
     * @param property the property represented in the diff
     * @param lineChange the type of change
     * @param lineNumber the line number
     */
    public EntityDiffCoordinate(String property, LineChange lineChange, int lineNumber)
    {
        this.property = property;
        this.lineChange = lineChange;
        this.lineNumber = lineNumber;
    }

    /**
     * @return the type of change.
     */
    public LineChange getLineChange()
    {
        return lineChange;
    }

    /**
     * @return the line number
     */
    public int getLineNumber()
    {
        return lineNumber;
    }

    /**
     * @return the property
     */
    public String getProperty()
    {
        return property;
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

        EntityDiffCoordinate that = (EntityDiffCoordinate) o;

        return new EqualsBuilder().append(lineNumber, that.lineNumber)
            .append(property, that.property).append(lineChange, that.lineChange).isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 97).append(property).append(lineChange).append(lineNumber).toHashCode();
    }
}
