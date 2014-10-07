package com.sequenceiq.cloudbreak.service.cluster.event;

import com.sequenceiq.cloudbreak.domain.StatusRequest;

public class ClusterStatusUpdateRequest {

    private Long stackId;
    private StatusRequest statusRequest;

    public ClusterStatusUpdateRequest(long stackId, StatusRequest statusRequest) {
        this.stackId = stackId;
        this.statusRequest = statusRequest;
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

}