package com.sequenceiq.cloudbreak.cloud.azure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ArmResourceTypeTest {

    @ParameterizedTest
    @EnumSource(ArmResourceType.class)
    void testGetByTypeRoundTrip(ArmResourceType resourceType) {
        assertEquals(Optional.of(resourceType), ArmResourceType.getByType(resourceType.getType()));
        assertTrue(resourceType.matches(resourceType.getType()));
    }

    @Test
    void testGetByTypeUnknownOrNull() {
        assertEquals(Optional.empty(), ArmResourceType.getByType("Microsoft.Unknown/things"));
        assertEquals(Optional.empty(), ArmResourceType.getByType("microsoft.network/loadbalancers"));
        assertEquals(Optional.empty(), ArmResourceType.getByType(null));
    }

    @Test
    void testMatches() {
        assertTrue(ArmResourceType.LOAD_BALANCER.matches("Microsoft.Network/loadBalancers"));
        assertFalse(ArmResourceType.LOAD_BALANCER.matches("Microsoft.Network/publicIPAddresses"));
        assertFalse(ArmResourceType.LOAD_BALANCER.matches(null));
    }
}

