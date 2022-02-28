package com.sequenceiq.cloudbreak.domain;

public enum BlueprintUpgradeOption {

    /**
     * Enabled means that a template can be upgraded with the CDP_RUNTIME_UPGRADE_DATAHUB entitlement. If this
     * entitlement is granted then we assume that all templates can be upgraded unless it is stated otherwise
     * in the template itself. Without the entitlement an upgrade can't be performed even if it is ENABLED.
     */
    ENABLED,

    /**
     * 'GA' means that a template can be upgraded without any entitlement to the version of the corresponding
     * data lake and nothing else
     */
    GA,

    /**
     * Disabled means that an upgrade can't be performed on a template. Neither Runtime nor OS upgrade. By default, with
     * the CDP_RUNTIME_UPGRADE_DATAHUB all templates can be upgraded so explicitly need to mark a template with
     * DISABLED to disable the upgrade. Without the entitlement only the MAINTENANCE_UPGRADE_GA marked templates
     * can be upgraded.
     */
    DISABLED,

    /**
     * On Data Hubs by default we perform a Runtime upgrade only. After the Runtime upgrade is complete, OS upgrade
     * can be performed as a separate step.
     */
    OS_UPGRADE_ENABLED,

    /**
     * On Data Hubs by default we perform a Runtime upgrade only. After the Runtime upgrade is complete, OS upgrade
     * can be performed as a separate step. To prevent the OS upgrade step, it can be disabled in the template.
     */
    OS_UPGRADE_DISABLED,

    /**
     * A template can be upgraded without the CDP_RUNTIME_UPGRADE_DATAHUB entitlement. Maintenance upgrade means
     * the 4th digit upgrade. For example 7.2.9-b1 -> 7.2.9-b2.
     */
    MAINTENANCE_UPGRADE_GA;

    public boolean isOsUpgradeEnabled() {
        return OS_UPGRADE_ENABLED.equals(this) || ENABLED.equals(this) || MAINTENANCE_UPGRADE_GA.equals(this);
    }

    public boolean isRuntimeUpgradeEnabled(boolean dataHubUpgradeEntitled) {
        return dataHubUpgradeEntitled ? !DISABLED.equals(this) : MAINTENANCE_UPGRADE_GA.equals(this);
    }
}
