package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sequenceiq.common.api.type.OutboundType;

class StackOutboundTypeValidationV4ResponseTest {

    @ParameterizedTest
    @MethodSource("noStacksFoundTestCases")
    void testConstructMessageNoStacksFound(Map<String, OutboundType> stackMap, String testDescription) {
        // When
        StackOutboundTypeValidationV4Response response = new StackOutboundTypeValidationV4Response(stackMap);

        // Then
        assertNotNull(response.getMessage());
        assertEquals("No stacks found.", response.getMessage());
    }

    static Stream<Arguments> noStacksFoundTestCases() {
        Map<String, OutboundType> nonUpgradeableMap = new HashMap<>();
        nonUpgradeableMap.put("stack1", OutboundType.LOAD_BALANCER);
        nonUpgradeableMap.put("stack2", OutboundType.PUBLIC_IP);
        nonUpgradeableMap.put("stack3", OutboundType.USER_ASSIGNED_NATGATEWAY);
        nonUpgradeableMap.put("stack4", OutboundType.USER_DEFINED_ROUTING);

        return Stream.of(
                Arguments.of(null, "null map"),
                Arguments.of(Collections.emptyMap(), "empty map"),
                Arguments.of(nonUpgradeableMap, "map with only non-upgradeable stacks")
        );
    }

    @ParameterizedTest
    @MethodSource("upgradeableStacksTestCases")
    void testConstructMessageWithUpgradeableStacks(Map<String, OutboundType> stackMap, String expectedMessage, String testDescription) {
        // When
        StackOutboundTypeValidationV4Response response = new StackOutboundTypeValidationV4Response(stackMap);

        // Then
        assertNotNull(response.getMessage());
        assertEquals(expectedMessage, response.getMessage());
    }

    static Stream<Arguments> upgradeableStacksTestCases() {
        Map<String, OutboundType> upgradeableOnlyMap = new LinkedHashMap<>();
        upgradeableOnlyMap.put("stack1", OutboundType.DEFAULT);
        upgradeableOnlyMap.put("stack2", OutboundType.NOT_DEFINED);

        Map<String, OutboundType> mixedMap = new LinkedHashMap<>();
        mixedMap.put("stack1", OutboundType.DEFAULT);
        mixedMap.put("stack2", OutboundType.LOAD_BALANCER);
        mixedMap.put("stack3", OutboundType.NOT_DEFINED);
        mixedMap.put("stack4", OutboundType.PUBLIC_IP);

        Map<String, OutboundType> singleStackMap = new HashMap<>();
        singleStackMap.put("my-stack", OutboundType.DEFAULT);

        return Stream.of(
                Arguments.of(upgradeableOnlyMap,
                    "The following stacks need to be upgraded: stack1 - DEFAULT; stack2 - NOT_DEFINED; ",
                    "upgradeable stacks only"),
                Arguments.of(mixedMap,
                    "The following stacks need to be upgraded: stack1 - DEFAULT; stack3 - NOT_DEFINED; ",
                    "mixed upgradeable and non-upgradeable stacks"),
                Arguments.of(singleStackMap,
                    "The following stacks need to be upgraded: my-stack - DEFAULT; ",
                    "single upgradeable stack")
        );
    }

    @Test
    void testConstructMessageWithAllOutboundTypes() {
        // Given - Test all OutboundType values to verify which are upgradeable
        Map<String, OutboundType> stackMap = new LinkedHashMap<>();
        stackMap.put("stack-load-balancer", OutboundType.LOAD_BALANCER);
        stackMap.put("stack-default", OutboundType.DEFAULT);
        stackMap.put("stack-not-defined", OutboundType.NOT_DEFINED);
        stackMap.put("stack-public-ip", OutboundType.PUBLIC_IP);
        stackMap.put("stack-user-assigned-natgateway", OutboundType.USER_ASSIGNED_NATGATEWAY);
        stackMap.put("stack-user-defined-routing", OutboundType.USER_DEFINED_ROUTING);

        // When
        StackOutboundTypeValidationV4Response response = new StackOutboundTypeValidationV4Response(stackMap);

        // Then
        assertNotNull(response.getMessage());
        assertEquals("The following stacks need to be upgraded: stack-default - DEFAULT; stack-not-defined - NOT_DEFINED; ",
                response.getMessage());
    }

    @Test
    void testSetStackOutboundTypeMapUpdatesMessage() {
        // Given
        StackOutboundTypeValidationV4Response response = new StackOutboundTypeValidationV4Response();
        Map<String, OutboundType> stackMap = new HashMap<>();
        stackMap.put("test-stack", OutboundType.DEFAULT);

        // When
        response.setStackOutboundTypeMap(stackMap);

        // Then
        assertNotNull(response.getMessage());
        assertEquals("The following stacks need to be upgraded: test-stack - DEFAULT; ", response.getMessage());
        assertEquals(stackMap, response.getStackOutboundTypeMap());
    }

}