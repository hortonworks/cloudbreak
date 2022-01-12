package com.sequenceiq.cloudbreak.cluster.model;

import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

public class ParcelOperationStatus {

    private Multimap<String, String> successful = HashMultimap.create();

    private Multimap<String, String> failed = HashMultimap.create();

    public ParcelOperationStatus() {
    }

    public ParcelOperationStatus(Map<String, String> successful, Map<String, String> failed) {
        this.successful = successful.entrySet().stream().collect(Multimaps.toMultimap(Map.Entry::getKey, Map.Entry::getValue, HashMultimap::create));
        this.failed = failed.entrySet().stream().collect(Multimaps.toMultimap(Map.Entry::getKey, Map.Entry::getValue, HashMultimap::create));
    }

    public Multimap<String, String> getSuccessful() {
        return successful;
    }

    public void setSuccessful(Multimap<String, String> successful) {
        this.successful = successful;
    }

    public Multimap<String, String> getFailed() {
        return failed;
    }

    public void setFailed(Multimap<String, String> failed) {
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
