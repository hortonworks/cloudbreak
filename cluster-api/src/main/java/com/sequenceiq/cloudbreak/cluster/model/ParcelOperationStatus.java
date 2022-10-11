package com.sequenceiq.cloudbreak.cluster.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

public class ParcelOperationStatus {

    private Map<String, String> successful = new HashMap<>();

    private Map<String, String> failed = new HashMap<>();

    public ParcelOperationStatus() {
    }

    public ParcelOperationStatus(Map<String, String> successful, Map<String, String> failed) {
        this.successful = new HashMap<>();
        for (Map.Entry<String, String> entry : successful.entrySet()) {
            this.successful.put(entry.getKey(), entry.getValue());
        }

        this.failed = new HashMap<>();
        for (Map.Entry<String, String> entry : failed.entrySet()) {
            this.failed.put(entry.getKey(), entry.getValue());
        }
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

    public void addSuccessful(String parcelName, String parcelVersion) {
        successful.put(parcelName, parcelVersion);
    }

    public ParcelOperationStatus withSuccessful(String parcelName, String parcelVersion) {
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
        operationStatus.failed.forEach((key, value) -> successful.remove(key, value));
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
