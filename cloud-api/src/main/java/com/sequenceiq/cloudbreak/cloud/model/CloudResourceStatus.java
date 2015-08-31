package com.sequenceiq.cloudbreak.cloud.model;

public class CloudResourceStatus {

    private CloudResource cloudResource;
    private ResourceStatus status;
    private String statusReason;
    private Long privateId;

    public CloudResourceStatus(CloudResource cloudResource, ResourceStatus status) {
        this(cloudResource, status, null);
    }

    public CloudResourceStatus(CloudResource cloudResource, ResourceStatus status, String statusReason) {
        this(cloudResource, status, statusReason, null);
    }

    public CloudResourceStatus(CloudResource cloudResource, ResourceStatus status, String statusReason, Long privateId) {
        this.cloudResource = cloudResource;
        this.status = status;
        this.statusReason = statusReason;
        this.privateId = privateId;
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

    public Long getPrivateId() {
        return privateId;
    }

    public void setPrivateId(Long privateId) {
        this.privateId = privateId;
    }

    public void setStatus(ResourceStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CloudResourceStatus{");
        sb.append("cloudResource=").append(cloudResource);
        sb.append(", status=").append(status);
        sb.append(", statusReason='").append(statusReason).append('\'');
        sb.append(", id=").append(privateId);
        sb.append('}');
        return sb.toString();
    }
}
