package com.sequenceiq.cloudbreak.domain.stack.cluster;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class InstanceStorageInfo {

    private int instanceStorageCount;

    private int instanceStorageSize;

    private boolean instanceStorageSupport;

    @JsonCreator
    public InstanceStorageInfo(@JsonProperty("instanceStorageSupport") boolean instanceStorageSupport,
            @JsonProperty("instanceStorageCount") int instanceStorageCount,
            @JsonProperty("instanceStorageSize") int instanceStorageSize) {
        this.instanceStorageSupport = instanceStorageSupport;
        this.instanceStorageCount = instanceStorageCount;
        this.instanceStorageSize = instanceStorageSize;
    }

    public int getInstanceStorageCount() {
        return instanceStorageCount;
    }

    public void setInstanceStorageCount(int instanceStorageCount) {
        this.instanceStorageCount = instanceStorageCount;
    }

    public int getInstanceStorageSize() {
        return instanceStorageSize;
    }

    public void setInstanceStorageSize(int instanceStorageSize) {
        this.instanceStorageSize = instanceStorageSize;
    }

    public boolean isInstanceStorageSupport() {
        return instanceStorageSupport;
    }

    public void setInstanceStorageSupport(boolean instanceStorageSupport) {
        this.instanceStorageSupport = instanceStorageSupport;
    }

    public String toString() {
        return "InstaceStorageInfo{" +
                "instanceStorageCount=" + instanceStorageCount +
                "instanceStorageSize=" + instanceStorageSize +
                "instanceStorageSupport=" + instanceStorageSupport + '}';
    }
}
