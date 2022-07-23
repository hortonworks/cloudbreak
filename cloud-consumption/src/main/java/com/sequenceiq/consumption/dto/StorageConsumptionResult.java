package com.sequenceiq.consumption.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class StorageConsumptionResult {

    private final double storageInBytes;

    @JsonCreator
    public StorageConsumptionResult(@JsonProperty("storageInBytes") double storageInBytes) {
        this.storageInBytes = storageInBytes;
    }

    public double getStorageInBytes() {
        return storageInBytes;
    }

    @Override
    public String toString() {
        return "StorageConsumptionResult{" +
                "storageInBytes=" + storageInBytes +
                '}';
    }
}
