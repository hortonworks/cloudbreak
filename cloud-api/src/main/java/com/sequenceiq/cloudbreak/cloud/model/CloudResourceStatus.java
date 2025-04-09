package com.sequenceiq.cloudbreak.cloud.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CloudResourceStatus {

    private final CloudResource cloudResource;

    private ResourceStatus status;

    private String statusReason;

    private Long privateId;

    public CloudResourceStatus(CloudResource cloudResource, ResourceStatus status) {
        this(cloudResource, status, null, null);
    }

    public CloudResourceStatus(CloudResource cloudResource, ResourceStatus status, Long privateId) {
        this(cloudResource, status, null, privateId);
    }

    public CloudResourceStatus(CloudResource cloudResource, ResourceStatus status, String statusReason) {
        this(cloudResource, status, statusReason, null);
    }

    @JsonCreator
    public CloudResourceStatus(
            @JsonProperty("cloudResource") CloudResource cloudResource,
            @JsonProperty("status") ResourceStatus status,
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("privateId") Long privateId) {

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

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
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

    public boolean isFailed() {
        return ResourceStatus.FAILED == status;
    }

    public boolean isDeleted() {
        return ResourceStatus.DELETED == status;
    }

    @Override
    public String toString() {
        return "CloudResourceStatus{" + "cloudResource=" + cloudResource +
                ", status=" + status +
                ", statusReason='" + statusReason + '\'' +
                ", id=" + privateId +
                '}';
    }
}