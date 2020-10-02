package com.sequenceiq.environment.parameters.dao.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class ResourceGroupUsagePatternTest {

    @ParameterizedTest
    @EnumSource(value = ResourceGroupUsagePattern.class, names = {"USE_SINGLE", "USE_SINGLE_WITH_DEDICATED_STORAGE_ACCOUNT"})
    void testIsSingleResourceGroupIsTrue(ResourceGroupUsagePattern resourceGroupUsagePattern) {
        assertTrue(resourceGroupUsagePattern.isSingleResourceGroup());
    }

    @ParameterizedTest
    @EnumSource(value = ResourceGroupUsagePattern.class, names = {"USE_MULTIPLE"})
    void testIsSingleResourceGroupIsFalse(ResourceGroupUsagePattern resourceGroupUsagePattern) {
        assertFalse(resourceGroupUsagePattern.isSingleResourceGroup());
    }
}
