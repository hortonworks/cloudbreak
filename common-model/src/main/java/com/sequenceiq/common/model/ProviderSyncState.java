package com.sequenceiq.common.model;

public enum ProviderSyncState {
    @Deprecated
    VALID,
    BASIC_SKU_MIGRATION_NEEDED,
    OUTBOUND_UPGRADE_NEEDED;
}
