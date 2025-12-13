package com.sequenceiq.cloudbreak.orchestrator.salt.utils;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;

class GrainsJsonPropertyUtilTest {

    private JsonNode testNode;

    private JsonNode testNodeEmpty;

    private Set<String> resultSet;

    @BeforeEach
    void setUp() throws IOException {
        testNode = JsonUtil.readTree("[\"ipa_member\", \"managet_agent\", \"kerberized\"]");
        testNodeEmpty = JsonUtil.readTree("");
        resultSet = Set.of("ipa_member", "managet_agent", "kerberized");
    }

    @Test
    void testGetProperties() {
        Set<String> result = GrainsJsonPropertyUtil.getPropertySet(testNode);
        assertTrue(result.containsAll(resultSet));
    }

    @Test
    void testGetPropertiesNullNode() {
        Set<String> result = GrainsJsonPropertyUtil.getPropertySet(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetPropertiesEmptyNode() {
        Set<String> result = GrainsJsonPropertyUtil.getPropertySet(testNodeEmpty);
        assertTrue(result.isEmpty());
    }

}