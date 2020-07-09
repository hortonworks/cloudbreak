package com.sequenceiq.common.api.tag.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.common.api.tag.model.Tags;
import com.sequenceiq.common.api.tag.request.TagsRequest;
import com.sequenceiq.common.api.tag.response.TagsResponse;

class TagsBaseTest {

    private static final String KEY = "key";

    private static final String VALUE = "value";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private TagsBase tags;

    @BeforeEach
    void setUp() {
        tags = new TagsBase() {
        };
    }

    @Test
    void addTag() {
        tags.addTag(KEY, VALUE);

        assertEquals(Map.of(KEY, VALUE), tags.getAll());
    }

    @Test
    void addTags() {
        tags.addTags(Map.of(KEY, VALUE));
        tags.addTags(new Tags(Map.of("key2", "value2")));

        assertEquals(Map.of(KEY, VALUE, "key2", "value2"), tags.getAll());
    }

    @Test
    void getTagValue() {
        tags.addTag(KEY, VALUE);

        assertEquals(VALUE, tags.getTagValue(KEY));
    }

    @Test
    void getAll() {
        tags.addTag(KEY, VALUE);

        assertEquals(Map.of(KEY, VALUE), tags.getAll());
    }

    @Test
    void getKeys() {
        tags.addTag(KEY, VALUE);

        assertEquals(Set.of(KEY), tags.getKeys());
    }

    @Test
    void getValues() {
        tags.addTag(KEY, VALUE);

        assertTrue(tags.getValues().contains(VALUE));
        assertFalse(tags.getValues().contains(KEY));
    }

    @Test
    void size() {
        tags.addTag(KEY, VALUE);

        assertEquals(1, tags.size());
    }

    @Test
    void isEmpty() {
        assertTrue(tags.isEmpty());

        tags.addTag(KEY, VALUE);
        assertFalse(tags.isEmpty());
    }

    @Test
    void hasTag() {
        assertFalse(tags.hasTag(KEY));

        tags.addTag(KEY, VALUE);
        assertTrue(tags.hasTag(KEY));
    }

    @ParameterizedTest
    @MethodSource("tagsClasses")
    <T extends TagsBase> void shouldSerializeAsMap(Class<T> tagClass) throws IOException {
        Map<String, String> map = Map.of(KEY, VALUE, "key2", "value2");

        String tagsJson = OBJECT_MAPPER.writeValueAsString(map);
        T tags = OBJECT_MAPPER.readValue(tagsJson, tagClass);

        assertEquals(map, tags.getAll());
    }

    @ParameterizedTest
    @MethodSource("tagsClasses")
    <T extends TagsBase> void shouldSerializeAndDeserializeCorrectly(Class<T> tagClass) throws Exception {
        T tags = tagClass.getConstructor().newInstance();
        tags.addTag(KEY, VALUE);
        tags.addTag("key2", "value2");

        String tagsJson = OBJECT_MAPPER.writeValueAsString(tags);
        T result = OBJECT_MAPPER.readValue(tagsJson, tagClass);

        assertEquals(tags, result);
    }

    static Stream<Arguments> tagsClasses() {
        return Stream.of(
                Arguments.of(Tags.class),
                Arguments.of(TagsRequest.class),
                Arguments.of(TagsResponse.class)
        );
    }
}
