package com.sequenceiq.cloudbreak.cloud.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StackTagsTest {

    private static final Map<String, String> USER_DEFINED_TAGS = new HashMap<>(Map.of("custom", "value"));

    private static final Map<String, String> APPLICATION_TAGS = new HashMap<>(Map.of("application", "app"));

    private static final Map<String, String> DEFAULT_TAGS = new HashMap<>(Map.of("owner", "john doe", "creation-timestamp", "1773042126"));

    private static final Map<String, String> UPDATED_USER_DEFINED_TAGS = Map.of("custom", "value2", "application", "app");

    private StackTags stackTags;

    @BeforeEach
    void setUp() {
        stackTags = new StackTags(USER_DEFINED_TAGS, APPLICATION_TAGS, DEFAULT_TAGS);
    }

    @Test
    void testUpdateUserDefinedTags() {
        stackTags.updateUserDefinedTags(UPDATED_USER_DEFINED_TAGS);

        assertEquals(APPLICATION_TAGS, stackTags.getApplicationTags());
        assertEquals(DEFAULT_TAGS, stackTags.getDefaultTags());
        assertEquals(UPDATED_USER_DEFINED_TAGS, stackTags.getUserDefinedTags());
    }

    @Test
    void testUpdateUserDefinedTagsWithNullMap() {
        stackTags.updateUserDefinedTags(null);

        assertEquals(APPLICATION_TAGS, stackTags.getApplicationTags());
        assertEquals(DEFAULT_TAGS, stackTags.getDefaultTags());
        assertEquals(USER_DEFINED_TAGS, stackTags.getUserDefinedTags());
    }

    @Test
    void getUserDefinedTagsWithoutDefaultTags() {
        assertEquals(Map.of("custom", "value"), stackTags.getUserDefinedTagsWithoutDefaultTags(Map.of("custom", "value", "owner", "jane doe")));
    }
}