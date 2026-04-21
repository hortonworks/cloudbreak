package com.sequenceiq.distrox.api.v1.distrox.model.upgrade;

public enum DistroXUpgradeReplaceVms {
    ENABLED,
    DISABLED;

    public static DistroXUpgradeReplaceVms fromBoolean(boolean value) {
        return value ? ENABLED : DISABLED;
    }
}
