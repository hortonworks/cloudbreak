package com.sequenceiq.cloudbreak.cloud.model;

public class CloudResourceStatus {

    private CloudResource cloudResource;

    private ResourceStatus status;

    private String statusReason;

    public CloudResourceStatus(CloudResource cloudResource, ResourceStatus status) {
        this(cloudResource, status, null);
    }

    public CloudResourceStatus(CloudResource cloudResource, ResourceStatus status, String statusReason) {
        this.cloudResource = cloudResource;
        this.status = status;
        this.statusReason = statusReason;
    }

    public CloudResource getCloudResource() {
        return cloudResource;
    }

    public ResourceStatus getStatus() {
        return status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    //BEGIN GENERATED CODE
    @Override
    public String toString() {
        return "CloudResourceStatus{" +
                "cloudResource=" + cloudResource +
                ", status=" + status +
                ", statusReason='" + statusReason + '\'' +
                '}';
    }
    //BEGIN GENERATED CODE
}
