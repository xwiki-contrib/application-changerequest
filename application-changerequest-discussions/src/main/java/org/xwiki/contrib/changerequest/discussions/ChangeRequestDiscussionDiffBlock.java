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
package org.xwiki.contrib.changerequest.discussions;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.contrib.changerequest.discussions.references.ChangeRequestLineDiffReference;
import org.xwiki.diff.display.UnifiedDiffBlock;

/**
 * This class is mostly a wrapper for a {@link org.xwiki.diff.display.UnifiedDiffBlock} but also contains some
 * information about the original localization of this diff block.
 * The goal is to be able to display a unified diff block outside of the global diff context, so with information from
 * where it was.
 *
 * @version $Id$
 * @since 1.5
 */
public class ChangeRequestDiscussionDiffBlock
{
    private final UnifiedDiffBlock<String, Character> diffBlock;
    private final ChangeRequestLineDiffReference reference;

    /**
     * Default constructor.
     *
     * @param diffBlock the actual diff block.
     * @param reference the reference where the diff comes from.
     */
    public ChangeRequestDiscussionDiffBlock(UnifiedDiffBlock<String, Character> diffBlock,
        ChangeRequestLineDiffReference reference)
    {
        this.diffBlock = diffBlock;
        this.reference = reference;
    }

    /**
     * @return the diff block to display.
     */
    public UnifiedDiffBlock<String, Character> getDiffBlock()
    {
        return diffBlock;
    }

    /**
     * @return the reference where the diff comes from.
     */
    public ChangeRequestLineDiffReference getReference()
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

        ChangeRequestDiscussionDiffBlock that = (ChangeRequestDiscussionDiffBlock) o;

        return new EqualsBuilder()
            .append(diffBlock, that.diffBlock)
            .append(reference, that.reference)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 63).append(diffBlock).append(reference).toHashCode();
    }
}
