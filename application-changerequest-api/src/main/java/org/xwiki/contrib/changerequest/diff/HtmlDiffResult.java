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
package org.xwiki.contrib.changerequest.diff;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Result of an HtmlDiff to be displayed.
 * This contains two parts: the actual diff to be displayed, and the list of required skins extensions (CSS, JS) to
 * be loaded for properly displaying the diff.
 *
 * @version $Id$
 * @since 1.22
 */
public class HtmlDiffResult
{
    private final String diff;
    private final String requiredSkinExtensions;

    /**
     * Default constructor.
     * @param diff the actual diff to display
     * @param requiredSkinExtensions the list of required skin extensions to be loaded
     */
    public HtmlDiffResult(String diff, String requiredSkinExtensions)
    {
        this.diff = diff;
        this.requiredSkinExtensions = requiredSkinExtensions;
    }

    /**
     * @return the diff to display
     */
    public String getDiff()
    {
        return diff;
    }

    /**
     * @return the list of required skin extensions to be loaded
     */
    public String getRequiredSkinExtensions()
    {
        return requiredSkinExtensions;
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

        HtmlDiffResult that = (HtmlDiffResult) o;

        return new EqualsBuilder().append(diff, that.diff)
            .append(requiredSkinExtensions, that.requiredSkinExtensions).isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 69).append(diff).append(requiredSkinExtensions).toHashCode();
    }
}
