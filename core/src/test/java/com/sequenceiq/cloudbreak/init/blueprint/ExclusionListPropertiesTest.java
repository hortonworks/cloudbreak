package com.sequenceiq.cloudbreak.init.blueprint;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExclusionListPropertiesTest {

    private ExclusionListProperties exclusionListProperties = new ExclusionListProperties();

    @BeforeEach
    public void setUp() {
        Map<String, String> exclusions = new HashMap<>();
        exclusions.put("1.0", "bp1,bp2");
        exclusions.put("2.0", "bp3,bp4");
        exclusions.put("default", "bp3,bp1");

        exclusionListProperties.setExclusions(exclusions);
        exclusionListProperties.parse();
    }

    @Test
    public void testDoesVersionFilterExist() {
        assertTrue(exclusionListProperties.doesVersionFilterExist("1.0"));
        assertFalse(exclusionListProperties.doesVersionFilterExist("3.0"));
    }

    @Test
    public void testBlueprintExcluded() {
        assertTrue(exclusionListProperties.isBlueprintExcluded("1.0", "bp1"));
        assertFalse(exclusionListProperties.isBlueprintExcluded("2.0", "bp5"));
    }

    @Test
    public void testBlueprintExcludedDefault() {
        assertFalse(exclusionListProperties.isBlueprintExcluded("3.0", "bp6"));
    }

}