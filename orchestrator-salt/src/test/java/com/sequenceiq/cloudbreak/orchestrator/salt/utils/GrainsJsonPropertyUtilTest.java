package com.sequenceiq.cloudbreak.orchestrator.salt.utils;

import java.io.IOException;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;

public class GrainsJsonPropertyUtilTest {

    private JsonNode testNode;

    private JsonNode testNodeEmpty;

    private Set<String> resultSet;

    @Before
    public void setUp() throws IOException {
        testNode = JsonUtil.readTree("[\"ipa_member\", \"managet_agent\", \"kerberized\"]");
        testNodeEmpty = JsonUtil.readTree("");
        resultSet = Set.of("ipa_member", "managet_agent", "kerberized");
    }

    @Test
    public void testGetProperties() {
        Set<String> result = GrainsJsonPropertyUtil.getPropertySet(testNode);
        Assert.assertTrue(result.containsAll(resultSet));
    }

    @Test
    public void testGetPropertiesNullNode() {
        Set<String> result = GrainsJsonPropertyUtil.getPropertySet(null);
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void testGetPropertiesEmptyNode() {
        Set<String> result = GrainsJsonPropertyUtil.getPropertySet(testNodeEmpty);
        Assert.assertTrue(result.isEmpty());
    }

}