package com.sequenceiq.cloudbreak.service.stack.event;

import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.api.model.StatusRequest;

public class StackStatusUpdateRequest {

    private Long stackId;
    private Platform cloudPlatform;
    private StatusRequest statusRequest;

    public StackStatusUpdateRequest(Platform cloudPlatform, Long stackId, StatusRequest statusRequest) {
        this.stackId = stackId;
        this.cloudPlatform = cloudPlatform;
        this.statusRequest = statusRequest;
    }

    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }

    public Platform getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(Platform cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public StatusRequest getStatusRequest() {
        return statusRequest;
    }

    public void setStatusRequest(StatusRequest statusRequest) {
        this.statusRequest = statusRequest;
    }

}