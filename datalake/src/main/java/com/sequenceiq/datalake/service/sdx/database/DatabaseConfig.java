package com.sequenceiq.datalake.service.sdx.database;

import static org.apache.commons.lang3.Validate.notBlank;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DatabaseConfig {

    private final String instanceType;

    private final String vendor;

    private final long volumeSize;

    @JsonCreator
    public DatabaseConfig(
            @JsonProperty("instanceType") String instanceType,
            @JsonProperty("vendor") String vendor,
            @JsonProperty("volumeSize") long volumeSize) {
        this.instanceType = notBlank(instanceType);
        this.vendor = notBlank(vendor);
        if (volumeSize <= 0) {
            throw new IllegalArgumentException("Volume size must be greater than zero");
        }
        this.volumeSize = volumeSize;
    }

    public String getInstanceType() {
        return instanceType;
    }

    public String getVendor() {
        return vendor;
    }

    public long getVolumeSize() {
        return volumeSize;
    }
}
