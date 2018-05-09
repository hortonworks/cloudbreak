package com.sequenceiq.cloudbreak.cloud.model;

public class CloudVmMetaDataStatus {

    private final CloudVmInstanceStatus cloudVmInstanceStatus;

    private final CloudInstanceMetaData metaData;

    public CloudVmMetaDataStatus(CloudVmInstanceStatus cloudVmInstanceStatus, CloudInstanceMetaData metaData) {
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
