package com.sequenceiq.common.model;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(with = JsonFormat.Feature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
public enum ProviderSyncState {
    @Deprecated
    @JsonEnumDefaultValue
    VALID,
    BASIC_SKU_MIGRATION_NEEDED,
    @Deprecated
    OUTBOUND_UPGRADE_NEEDED,
    DISK_MISMATCH_FOUND;
}
