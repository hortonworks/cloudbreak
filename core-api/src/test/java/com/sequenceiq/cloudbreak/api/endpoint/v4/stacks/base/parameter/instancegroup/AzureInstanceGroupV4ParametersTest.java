package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AzureInstanceGroupV4ParametersTest {

    private AzureInstanceGroupV4Parameters underTest;

    @BeforeEach
    void setUp() {
        underTest = new AzureInstanceGroupV4Parameters();
    }

    @Test
    void asMapTest() {
        AzureAvailabiltySetV4 availabilitySet = new AzureAvailabiltySetV4();
        availabilitySet.setName("TEST");
        availabilitySet.setFaultDomainCount(2);
        availabilitySet.setUpdateDomainCount(3);
        underTest.setAvailabilitySet(availabilitySet);

        Map<String, Object> response = underTest.asMap();
        assertEquals("AZURE", response.get("cloudPlatform"));
        Map<String, Object> responseAvailabilityTest = (Map<String, Object>) response.get("availabilitySet");
        assertEquals("TEST", responseAvailabilityTest.get("name"));
        assertEquals(2, responseAvailabilityTest.get("faultDomainCount"));
        assertEquals(3, responseAvailabilityTest.get("updateDomainCount"));
        assertFalse(responseAvailabilityTest.containsKey("cloudPlatform"));
    }

    @Test
    void asMapEmptyAvailabilitySetTest() {
        Map<String, Object> response = underTest.asMap();
        Map<String, Object> responseAvailabilityTest = (Map<String, Object>) response.get("availabilitySet");
        assertNull(responseAvailabilityTest);
    }

    @Test
    void parseTest() {
        Map<String, Object> availabilitySetMap = Map.of("availabilitySet", Map.of("name", "TEST", "faultDomainCount", 2,
                "updateDomainCount", 3));
        underTest.parse(availabilitySetMap);

        AzureAvailabiltySetV4 availabiltySetV4 = underTest.getAvailabilitySet();
        assertEquals("TEST", availabiltySetV4.getName());
        assertEquals(2, availabiltySetV4.getFaultDomainCount());
        assertEquals(3, availabiltySetV4.getUpdateDomainCount());
    }

    @Test
    void parseEmptyMapTest() {
        Map<String, Object> availabilitySetMap = Map.of("availabilitySet", Map.of());
        underTest.parse(availabilitySetMap);

        assertNull(underTest.getAvailabilitySet());
    }

    @Test
    void parseNullMapTest() {
        underTest.parse(null);

        assertNull(underTest.getAvailabilitySet());
    }
}
