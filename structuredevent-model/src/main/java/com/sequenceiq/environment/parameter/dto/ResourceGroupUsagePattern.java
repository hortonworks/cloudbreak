package com.sequenceiq.environment.parameter.dto;

public enum ResourceGroupUsagePattern {
    USE_MULTIPLE,
    USE_SINGLE,
    USE_SINGLE_WITH_DEDICATED_STORAGE_ACCOUNT;

    public boolean isSingleResourceGroup() {
        return USE_SINGLE.equals(this) || USE_SINGLE_WITH_DEDICATED_STORAGE_ACCOUNT.equals(this);
    }
}
