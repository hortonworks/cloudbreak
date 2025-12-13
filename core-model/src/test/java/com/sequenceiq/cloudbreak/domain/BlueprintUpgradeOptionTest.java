package com.sequenceiq.cloudbreak.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

public class BlueprintUpgradeOptionTest {

    @Test
    void testIsOsUpgradeEnabled() {
        assertAll(
                BlueprintUpgradeOption.OS_UPGRADE_ENABLED::isOsUpgradeEnabled,
                BlueprintUpgradeOption.ENABLED::isOsUpgradeEnabled,
                BlueprintUpgradeOption.MAINTENANCE_UPGRADE_GA::isOsUpgradeEnabled);
    }

    @Test
    void testIsOsUpgradeEnabledForInvalidStates() {
        assertFalse(BlueprintUpgradeOption.OS_UPGRADE_DISABLED.isOsUpgradeEnabled());
        assertFalse(BlueprintUpgradeOption.DISABLED.isOsUpgradeEnabled());
    }
}