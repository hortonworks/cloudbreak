package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class AvailabilityTypeTest {

    @ParameterizedTest
    @MethodSource(value = "nodeCountToAvailabilityType")
    void testGetByInstanceCount(int nodeCount, AvailabilityType availabilityType) {
        assertEquals(availabilityType, AvailabilityType.getByInstanceCount(nodeCount));
    }

    static Object[][] nodeCountToAvailabilityType() {
        return new Object[][]{
                {0, AvailabilityType.NON_HA },
                {1, AvailabilityType.NON_HA },
                {2, AvailabilityType.TWO_NODE_BASED },
                {3, AvailabilityType.HA },
                {4, AvailabilityType.HA },
                {5, AvailabilityType.HA },
        };
    }

}