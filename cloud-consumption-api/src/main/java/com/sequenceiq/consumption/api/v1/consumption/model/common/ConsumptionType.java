package com.sequenceiq.consumption.api.v1.consumption.model.common;

import com.sequenceiq.cloudbreak.common.mappable.StorageType;

public enum ConsumptionType {

    UNKNOWN(StorageType.UNKNOWN),
    STORAGE(StorageType.S3),
    ELASTIC_FILESYSTEM(StorageType.EFS),
    EBS(StorageType.EBS);

    private final StorageType storageType;

    ConsumptionType(StorageType storageType) {
        this.storageType = storageType;
    }

    public StorageType getStorageService() {
        return storageType;
    }
}
