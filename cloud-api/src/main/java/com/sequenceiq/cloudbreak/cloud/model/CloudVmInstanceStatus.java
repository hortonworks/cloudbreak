package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Objects;

public class CloudVmInstanceStatus {

    private final CloudInstance cloudInstance;

    private final InstanceStatus status;

    private final String statusReason;

    public CloudVmInstanceStatus(CloudInstance cloudInstance, InstanceStatus status, String statusReason) {
        this.cloudInstance = cloudInstance;
        this.status = status;
        this.statusReason = statusReason;
    }

    public CloudVmInstanceStatus(CloudInstance cloudInstance, InstanceStatus status) {
        this(cloudInstance, status, null);
    }

    public CloudInstance getCloudInstance() {
        return cloudInstance;
    }

    public InstanceStatus getStatus() {
        return status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    @Override
    public String toString() {
        return "CloudVmInstanceStatus{"
                + "instance=" + cloudInstance
                + ", status=" + status
                + ", statusReason='" + statusReason + '\''
                + '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        CloudVmInstanceStatus other = (CloudVmInstanceStatus) obj;

        return Objects.equals(cloudInstance, other.cloudInstance)
                && Objects.equals(status, other.status)
                && Objects.equals(statusReason, other.statusReason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), cloudInstance, status, statusReason);
    }
}
