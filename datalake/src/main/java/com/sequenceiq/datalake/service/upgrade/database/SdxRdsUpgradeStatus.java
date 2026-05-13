package com.sequenceiq.datalake.service.upgrade.database;

public enum SdxRdsUpgradeStatus {
    UPGRADE_REQUIRED,
    UPGRADE_NOT_REQUIRED,
    UNKNOWN,
    NO_DATALAKE
}
