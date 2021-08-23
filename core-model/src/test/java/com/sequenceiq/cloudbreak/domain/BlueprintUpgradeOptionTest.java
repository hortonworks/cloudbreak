package com.sequenceiq.cloudbreak.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class BlueprintUpgradeOptionTest {

    @Test
    @DisplayName("major runtime upgrade is enabled with the entitlement and enabled option")
    void testIsRuntimeUpgradeEnabled1() {
        assertTrue(BlueprintUpgradeOption.ENABLED.isRuntimeUpgradeEnabled(true));
    }

    @Test
    @DisplayName("major runtime upgrade is enabled with the entitlement and maintenance upgrade option")
    void testIsRuntimeUpgradeEnabled2() {
        assertTrue(BlueprintUpgradeOption.MAINTENANCE_UPGRADE_GA.isRuntimeUpgradeEnabled(true));
    }

    @Test
    @DisplayName("major runtime upgrade is enabled with the entitlement and OS upgrade enabled option")
    void testIsRuntimeUpgradeEnabled3() {
        assertTrue(BlueprintUpgradeOption.OS_UPGRADE_ENABLED.isRuntimeUpgradeEnabled(true));
    }

    @Test
    @DisplayName("major runtime upgrade is enabled with the entitlement and OS upgrade disabled option")
    void testIsRuntimeUpgradeEnabled4() {
        assertTrue(BlueprintUpgradeOption.OS_UPGRADE_DISABLED.isRuntimeUpgradeEnabled(true));
    }

    @Test
    @DisplayName("major runtime upgrade is disabled with the entitlement and disabled option")
    void testIsRuntimeUpgradeEnabled5() {
        assertFalse(BlueprintUpgradeOption.DISABLED.isRuntimeUpgradeEnabled(true));
    }

    @Test
    @DisplayName("maintenance upgrade is enabled without the entitlement and maintenance upgrade option")
    void testIsRuntimeUpgradeEnabled6() {
        assertTrue(BlueprintUpgradeOption.MAINTENANCE_UPGRADE_GA.isRuntimeUpgradeEnabled(false));
    }

    @Test
    @DisplayName("upgrade is disabled without the entitlement and enabled upgrade option")
    void testIsRuntimeUpgradeEnabled7() {
        assertFalse(BlueprintUpgradeOption.ENABLED.isRuntimeUpgradeEnabled(false));
    }

    @Test
    @DisplayName("upgrade is disabled without the entitlement and OS upgrade enabled upgrade option")
    void testIsRuntimeUpgradeEnabled8() {
        assertFalse(BlueprintUpgradeOption.OS_UPGRADE_ENABLED.isRuntimeUpgradeEnabled(false));
    }

    @Test
    void testIsOsUpgradeEnabled() {
        Assertions.assertAll(
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