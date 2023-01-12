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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.diff.display.InlineDiffChunk;
import org.xwiki.diff.display.UnifiedDiffBlock;
import org.xwiki.diff.display.UnifiedDiffElement;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;

import static com.fasterxml.jackson.databind.MapperFeature.SORT_PROPERTIES_ALPHABETICALLY;

/**
 * Utility component for manipulating {@link UnifiedDiffBlock}.
 * This component mainly provides helpers to serialize/deserialize to/from JSON.
 *
 * @version $Id$
 * @since 1.5
 */
@Component(roles = { ChangeRequestDiscussionDiffUtils.class})
@Singleton
public class ChangeRequestDiscussionDiffUtils
{
    private static final String TYPE_FIELD = "type";

    private ObjectMapper objectMapper;

    /**
     * Specific Jackson deserializer for {@link UnifiedDiffBlock}.
     *
     * @version $Id$
     * @since 1.5
     */
    private static class UnifiedDiffBlockDeserializer extends JsonDeserializer<UnifiedDiffBlock<String, Character>>
    {
        @Override
        public UnifiedDiffBlock<String, Character> deserialize(JsonParser jsonParser,
            DeserializationContext deserializationContext)
            throws IOException, JacksonException
        {
            UnifiedDiffBlock<String, Character> result = new UnifiedDiffBlock<>();
            JsonNode jsonNode = jsonParser.getCodec().readTree(jsonParser);
            if (jsonNode.isArray()) {
                ArrayNode jsonArray = (ArrayNode) jsonNode;
                for (JsonNode node : jsonArray) {
                    UnifiedDiffElement<String, Character> diffElement =
                        deserializationContext.readTreeAsValue(node, UnifiedDiffElement.class);
                    result.add(diffElement);
                }
            }

            return result;
        }
    }

    /**
     * Specific Jackson Deserializer for {@link UnifiedDiffElement}.
     *
     * @version $Id$
     * @since 1.5
     */
    private static class UnifiedDiffElementDeserializer extends JsonDeserializer<UnifiedDiffElement<String, Character>>
    {
        @Override
        public UnifiedDiffElement<String, Character> deserialize(JsonParser jsonParser,
            DeserializationContext deserializationContext) throws IOException, JacksonException
        {
            JsonNode jsonNode = jsonParser.getCodec().readTree(jsonParser);
            int index = jsonNode.get("index").asInt();
            String type = jsonNode.get(TYPE_FIELD).asText();
            String value = jsonNode.get("value").asText();
            UnifiedDiffElement<String, Character> diffElement = new UnifiedDiffElement<>(index,
                UnifiedDiffElement.Type.valueOf(type), value);

            JsonNode chunksNode = jsonNode.get("chunks");
            if (chunksNode.isArray()) {
                ArrayNode chunksArray = (ArrayNode) chunksNode;
                List<InlineDiffChunk<Character>> chunkList = new ArrayList<>();
                for (JsonNode chunkNode : chunksArray) {
                    InlineDiffChunk inlineDiffChunk =
                        deserializationContext.readTreeAsValue(chunkNode, InlineDiffChunk.class);
                    chunkList.add(inlineDiffChunk);
                }
                diffElement.setChunks(chunkList);
            }
            return diffElement;
        }
    }

    /**
     * Specific Jackson deserializer for {@link InlineDiffChunk}.
     *
     * @version $Id$
     * @since 1.5
     */
    private static class InlineDiffChunkElementDeserializer extends JsonDeserializer<InlineDiffChunk<Character>>
    {
        @Override
        public InlineDiffChunk<Character> deserialize(JsonParser jsonParser,
            DeserializationContext deserializationContext) throws IOException, JacksonException
        {
            JsonNode jsonNode = jsonParser.getCodec().readTree(jsonParser);
            String type = jsonNode.get(TYPE_FIELD).asText();
            JsonNode elements = jsonNode.get("elements");
            List<Character> characterList = new ArrayList<>();
            if (elements.isArray()) {
                ArrayNode elementsArray = (ArrayNode) elements;
                for (JsonNode element : elementsArray) {
                    String text = element.asText();
                    if (text.length() != 1) {
                        throw new JsonParseException(jsonParser,
                            String.format("elements should be an array of characters: found a "
                            + "string value: %s", text));
                    }
                    characterList.add(text.charAt(0));
                }
            }

            return new InlineDiffChunk<>(InlineDiffChunk.Type.valueOf(type), characterList);
        }
    }

    private ObjectMapper getObjectMapper()
    {
        if (this.objectMapper == null) {
            SimpleModule module = new SimpleModule();
            module.addDeserializer(UnifiedDiffBlock.class, new UnifiedDiffBlockDeserializer());
            module.addDeserializer(UnifiedDiffElement.class, new UnifiedDiffElementDeserializer());
            module.addDeserializer(InlineDiffChunk.class, new InlineDiffChunkElementDeserializer());
            this.objectMapper = JsonMapper.builder()
                // Order properties alphabetically: easier to test the result.
                .enable(SORT_PROPERTIES_ALPHABETICALLY)
                // if for some reason we have more info in the serialization, we don't want it to fail the
                // deserialization
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .addModule(module)
                .build();
        }
        return this.objectMapper;
    }

    /**
     * Perform serialization of the given block to JSON.
     *
     * @param unifiedDiffBlock the block to serialize
     * @return a JSON representing the block containing in a string
     * @throws JsonProcessingException in case of problem during serialization
     */
    public String serialize(UnifiedDiffBlock<String, Character> unifiedDiffBlock) throws JsonProcessingException
    {
        return getObjectMapper().writeValueAsString(unifiedDiffBlock);
    }

    /**
     * Performs deserialization of a JSON to obtain a {@link UnifiedDiffBlock}.
     *
     * @param jsonSerialization a serialized {@link UnifiedDiffBlock}
     * @return a {@link UnifiedDiffBlock} built from the given serialization
     * @throws JsonProcessingException in case of problem during deserialization
     */
    public UnifiedDiffBlock<String, Character> deserialize(String jsonSerialization) throws JsonProcessingException
    {
        return getObjectMapper().readValue(jsonSerialization, UnifiedDiffBlock.class);
    }
}
