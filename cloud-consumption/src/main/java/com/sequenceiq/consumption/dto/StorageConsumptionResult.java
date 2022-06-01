package com.sequenceiq.consumption.dto;

public class StorageConsumptionResult {

    private final double storageInBytes;

    public StorageConsumptionResult(double storageInBytes) {
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
