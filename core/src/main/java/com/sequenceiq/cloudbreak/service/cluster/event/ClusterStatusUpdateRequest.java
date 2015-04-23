package com.sequenceiq.cloudbreak.service.cluster.event;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.StatusRequest;

public class ClusterStatusUpdateRequest {

    private Long stackId;
    private StatusRequest statusRequest;
    private CloudPlatform cloudPlatform;

    public ClusterStatusUpdateRequest(long stackId, StatusRequest statusRequest, CloudPlatform cloudPlatform) {
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

    public CloudPlatform getCloudPlatform() {
        return cloudPlatform;
    }
}