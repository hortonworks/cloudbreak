package com.sequenceiq.cloudbreak.cloud.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CloudVmMetaDataStatus {

    private final CloudVmInstanceStatus cloudVmInstanceStatus;

    private final CloudInstanceMetaData metaData;

    @JsonCreator
    public CloudVmMetaDataStatus(@JsonProperty("cloudVmInstanceStatus") CloudVmInstanceStatus cloudVmInstanceStatus,
            @JsonProperty("metaData") CloudInstanceMetaData metaData) {
        this.cloudVmInstanceStatus = cloudVmInstanceStatus;
        this.metaData = metaData;
    }

    public CloudVmInstanceStatus getCloudVmInstanceStatus() {
        return cloudVmInstanceStatus;
    }

    public CloudInstanceMetaData getMetaData() {
        return metaData;
    }

    @Override
    public String toString() {
        return "CloudVmMetaDataStatus{"
                + "cloudVmInstanceStatus=" + cloudVmInstanceStatus
                + ", metaData=" + metaData
                + '}';
    }
}
