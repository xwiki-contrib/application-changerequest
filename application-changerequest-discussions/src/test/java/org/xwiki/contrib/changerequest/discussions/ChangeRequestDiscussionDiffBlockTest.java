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

import org.junit.jupiter.api.Test;
import org.xwiki.contrib.changerequest.discussions.references.ChangeRequestLineDiffReference;
import org.xwiki.diff.display.UnifiedDiffBlock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ChangeRequestDiscussionDiffBlock}.
 *
 * @version $Id$
 */
class ChangeRequestDiscussionDiffBlockTest
{
    @Test
    void getters()
    {
        UnifiedDiffBlock<String, Character> diffBlock = new UnifiedDiffBlock<>();
        ChangeRequestLineDiffReference reference = mock(ChangeRequestLineDiffReference.class);
        ChangeRequestDiscussionDiffBlock block = new ChangeRequestDiscussionDiffBlock(diffBlock, reference);

        assertEquals(diffBlock, block.getDiffBlock());
        assertEquals(reference, block.getReference());
    }

    @Test
    void equalsAndHashCode()
    {
        UnifiedDiffBlock<String, Character> diffBlock = new UnifiedDiffBlock<>();
        ChangeRequestLineDiffReference reference = mock(ChangeRequestLineDiffReference.class);
        ChangeRequestDiscussionDiffBlock block = new ChangeRequestDiscussionDiffBlock(diffBlock, reference);

        assertEquals(block, block);
        assertNotEquals(block, null);
        assertNotEquals(block, "somethingelse");

        ChangeRequestDiscussionDiffBlock sameFields = new ChangeRequestDiscussionDiffBlock(diffBlock, reference);
        assertEquals(block, sameFields);
        assertEquals(block.hashCode(), sameFields.hashCode());

        ChangeRequestDiscussionDiffBlock otherReference =
            new ChangeRequestDiscussionDiffBlock(diffBlock, mock(ChangeRequestLineDiffReference.class));
        assertNotEquals(block, otherReference);

        UnifiedDiffBlock<String, Character> otherDiffBlock = new UnifiedDiffBlock<>();
        otherDiffBlock.add(null);
        ChangeRequestDiscussionDiffBlock otherDiffBlockValue =
            new ChangeRequestDiscussionDiffBlock(otherDiffBlock, reference);
        assertNotEquals(block, otherDiffBlockValue);
    }
}
