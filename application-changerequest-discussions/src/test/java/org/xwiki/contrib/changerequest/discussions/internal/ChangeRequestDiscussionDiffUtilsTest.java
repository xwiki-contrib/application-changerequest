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
package org.xwiki.contrib.changerequest.discussions.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Test;
import org.xwiki.diff.display.InlineDiffChunk;
import org.xwiki.diff.display.UnifiedDiffBlock;
import org.xwiki.diff.display.UnifiedDiffElement;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import com.fasterxml.jackson.core.JsonProcessingException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ChangeRequestDiscussionDiffUtils}.
 *
 * @version $Id$
 * @since 1.5
 */
@ComponentTest
class ChangeRequestDiscussionDiffUtilsTest
{
    @InjectMockComponents
    private ChangeRequestDiscussionDiffUtils discussionDiffUtils;

    private static final String JSON_EXAMPLE =
        "[{\"added\":false,\"chunks\":null,\"deleted\":false,\"index\":14,\"type\":\"CONTEXT\",\"value\":"
            + "\"Lorem ipsum dolor sit amet, consectetur\"},"
            + "{\"added\":false,\"chunks\":null,\"deleted\":false,\"index\":15,\"type\":\"CONTEXT\",\"value\":\"\"},"
            + "{\"added\":false,\"chunks\":["
            + "{\"added\":false,\"deleted\":true,\"elements\":[\"=\",\"=\",\" \"],\"type\":\"DELETED\","
            + "\"unmodified\":false},"
            + "{\"added\":false,\"deleted\":false,\"elements\":[\"S\"],\"type\":\"UNMODIFIED\","
            + "\"unmodified\":true},"
            + "{\"added\":false,\"deleted\":true,\"elements\":[\"u\",\"b\",\"-\"],\"type\":\"DELETED\","
            + "\"unmodified\":false},"
            + "{\"added\":false,\"deleted\":false,\"elements\":[\"p\",\"a\",\"r\",\"a\",\"g\",\"r\",\"a\",\"p\",\"h\"],"
            + "\"type\":\"UNMODIFIED\",\"unmodified\":true},"
            + "{\"added\":false,\"deleted\":true,\"elements\":[\" \",\"=\",\"=\"],\"type\":\"DELETED\","
            + "\"unmodified\":false}],"
            + "\"deleted\":true,\"index\":16,\"type\":\"DELETED\",\"value\":\"== Sub-paragraph ==\"},"
            + "{\"added\":true,\"chunks\":["
            + "{\"added\":false,\"deleted\":false,\"elements\":[\"S\"],\"type\":\"UNMODIFIED\","
            + "\"unmodified\":true},"
            + "{\"added\":true,\"deleted\":false,\"elements\":[\"o\",\"m\",\"e\",\" \",\"c\",\"h\",\"a\",\"n\",\"g\","
            + "\"e\",\"s\",\" \",\"i\",\"n\",\" \",\"t\",\"h\",\"i\",\"s\",\" \"],\"type\":\"ADDED\","
            + "\"unmodified\":false},"
            + "{\"added\":false,\"deleted\":false,\"elements\":[\"p\",\"a\",\"r\",\"a\",\"g\",\"r\",\"a\",\"p\",\"h\"],"
            + "\"type\":\"UNMODIFIED\",\"unmodified\":true},"
            + "{\"added\":true,\"deleted\":false,\"elements\":[\".\"],\"type\":\"ADDED\",\"unmodified\":false}],"
            + "\"deleted\":false,\"index\":18,\"type\":\"ADDED\",\"value\":\"Some changes in this paragraph.\"},"
            + "{\"added\":false,\"chunks\":null,\"deleted\":false,\"index\":17,\"type\":\"CONTEXT\",\"value\":\"\"},"
            + "{\"added\":false,\"chunks\":null,\"deleted\":true,\"index\":18,\"type\":\"DELETED\","
            + "\"value\":\"Lorem ipsum dolor sit amet, consectetur adipiscing elit\"},"
            + "{\"added\":false,\"chunks\":null,\"deleted\":true,\"index\":19,\"type\":\"DELETED\",\"value\":\"\"}]";

    private List<Character> stringToListChar(String string)
    {
        return Arrays.asList(ArrayUtils.toObject(string.toCharArray()));
    }

    private UnifiedDiffBlock<String, Character> getUnidifiedDiffBlock()
    {
        UnifiedDiffBlock<String, Character> block = new UnifiedDiffBlock<>();
        UnifiedDiffElement<String, Character> element;
        InlineDiffChunk<Character> chunk;
        List<InlineDiffChunk<Character>> chunks;

        element = new UnifiedDiffElement<>(14, UnifiedDiffElement.Type.CONTEXT, "Lorem ipsum dolor sit amet, "
            + "consectetur");
        block.add(element);

        element = new UnifiedDiffElement<>(15, UnifiedDiffElement.Type.CONTEXT, "");
        block.add(element);

        element = new UnifiedDiffElement<>(16, UnifiedDiffElement.Type.DELETED, "== Sub-paragraph ==");
        chunks = new ArrayList<>();
        chunk = new InlineDiffChunk<>(InlineDiffChunk.Type.DELETED, this.stringToListChar("== "));
        chunks.add(chunk);
        chunk = new InlineDiffChunk<>(InlineDiffChunk.Type.UNMODIFIED, this.stringToListChar("S"));
        chunks.add(chunk);
        chunk = new InlineDiffChunk<>(InlineDiffChunk.Type.DELETED, this.stringToListChar("ub-"));
        chunks.add(chunk);
        chunk = new InlineDiffChunk<>(InlineDiffChunk.Type.UNMODIFIED, this.stringToListChar("paragraph"));
        chunks.add(chunk);
        chunk = new InlineDiffChunk<>(InlineDiffChunk.Type.DELETED, this.stringToListChar(" =="));
        chunks.add(chunk);
        element.setChunks(chunks);
        block.add(element);

        element = new UnifiedDiffElement<>(18, UnifiedDiffElement.Type.ADDED, "Some changes in this paragraph.");
        chunks = new ArrayList<>();
        chunk = new InlineDiffChunk<>(InlineDiffChunk.Type.UNMODIFIED, this.stringToListChar("S"));
        chunks.add(chunk);
        chunk = new InlineDiffChunk<>(InlineDiffChunk.Type.ADDED, this.stringToListChar("ome changes in this "));
        chunks.add(chunk);
        chunk = new InlineDiffChunk<>(InlineDiffChunk.Type.UNMODIFIED, this.stringToListChar("paragraph"));
        chunks.add(chunk);
        chunk = new InlineDiffChunk<>(InlineDiffChunk.Type.ADDED, this.stringToListChar("."));
        chunks.add(chunk);
        element.setChunks(chunks);
        block.add(element);

        element = new UnifiedDiffElement<>(17, UnifiedDiffElement.Type.CONTEXT, "");
        block.add(element);

        element = new UnifiedDiffElement<>(18, UnifiedDiffElement.Type.DELETED,
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit");
        block.add(element);

        element = new UnifiedDiffElement<>(19, UnifiedDiffElement.Type.DELETED, "");
        block.add(element);

        return block;
    }

    @Test
    void jsonSerializer() throws JsonProcessingException
    {
        String jsonSerialization = this.discussionDiffUtils.serialize(getUnidifiedDiffBlock());
        assertEquals(JSON_EXAMPLE, jsonSerialization);
    }

    @Test
    void jsonDeserializer() throws JsonProcessingException
    {
        UnifiedDiffBlock<String, Character> diffBlock = this.discussionDiffUtils.deserialize(JSON_EXAMPLE);
        // We cannot perform assertEquals because those classes are missing equals method...
        // assertEquals(getUnidifiedDiffBlock(), diffBlock);
        String serializedJson = this.discussionDiffUtils.serialize(diffBlock);
        assertEquals(JSON_EXAMPLE, serializedJson);
    }
}