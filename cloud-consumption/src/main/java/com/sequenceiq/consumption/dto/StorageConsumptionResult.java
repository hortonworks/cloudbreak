package com.sequenceiq.consumption.dto;

import java.util.Objects;

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
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StorageConsumptionResult that = (StorageConsumptionResult) o;
        return Double.compare(that.storageInBytes, storageInBytes) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(storageInBytes);
    }

    @Override
    public String toString() {
        return "StorageConsumptionResult{" +
                "storageInBytes=" + storageInBytes +
                '}';
    }
}
