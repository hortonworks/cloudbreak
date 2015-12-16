package com.sequenceiq.cloudbreak.service.cluster.event;

import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.common.type.StatusRequest;

public class ClusterStatusUpdateRequest {

    private Long stackId;
    private StatusRequest statusRequest;
    private Platform cloudPlatform;

    public ClusterStatusUpdateRequest(long stackId, StatusRequest statusRequest, Platform cloudPlatform) {
        this.stackId = stackId;
        this.statusRequest = statusRequest;
        this.cloudPlatform = cloudPlatform;
    }

    public long getStackId() {
        return stackId;
    }

    public void setStackId(long stackId) {
        this.stackId = stackId;
    }

    public StatusRequest getStatusRequest() {
        return statusRequest;
    }

    public void setStatusRequest(StatusRequest statusRequest) {
        this.statusRequest = statusRequest;
    }

    public Platform getCloudPlatform() {
        return cloudPlatform;
    }
}