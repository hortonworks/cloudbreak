package com.sequenceiq.cloudbreak.cluster.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

public class ParcelOperationStatus {

    private Map<String, String> successful = new HashMap<>();

    private Map<String, String> failed = new HashMap<>();

    public ParcelOperationStatus() {
    }

    public ParcelOperationStatus(Map<String, String> successful, Map<String, String> failed) {
        this.successful = new HashMap<>(successful);
        this.failed = new HashMap<>(failed);
    }

    public Map<String, String> getSuccessful() {
        return successful;
    }

    public void setSuccessful(Map<String, String> successful) {
        this.successful = successful;
    }

    public Map<String, String> getFailed() {
        return failed;
    }

    public void setFailed(Map<String, String> failed) {
        this.failed = failed;
    }

    public void addSuccesful(String parcelName, String parcelVersion) {
        successful.put(parcelName, parcelVersion);
    }

    public ParcelOperationStatus withSuccesful(String parcelName, String parcelVersion) {
        successful.put(parcelName, parcelVersion);
        return this;
    }

    public void addFailed(String parcelName, String parcelVersion) {
        failed.put(parcelName, parcelVersion);
    }

    public ParcelOperationStatus withFailed(String parcelName, String parcelVersion) {
        failed.put(parcelName, parcelVersion);
        return this;
    }

    public ParcelOperationStatus merge(ParcelOperationStatus operationStatus) {
        successful.putAll(operationStatus.successful);
        failed.putAll(operationStatus.failed);
        successful.entrySet().removeIf(entry -> failed.containsKey(entry.getKey()));
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ParcelOperationStatus that = (ParcelOperationStatus) o;
        return Objects.equals(successful, that.successful) && Objects.equals(failed, that.failed);
    }

    @Override
    public int hashCode() {
        return Objects.hash(successful, failed);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ParcelOperationStatus.class.getSimpleName() + "[", "]")
                .add("successful=" + successful)
                .add("failed=" + failed)
                .toString();
    }
}
