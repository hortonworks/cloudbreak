package com.sequenceiq.sdx.api.model;

public enum SdxUpgradeReplaceVms {

    ENABLED(true),
    DISABLED(false);

    private final boolean booleanValue;

    SdxUpgradeReplaceVms(boolean booleanValue) {
        this.booleanValue = booleanValue;
    }

    public boolean getBooleanValue() {
        return booleanValue;
    }
}
