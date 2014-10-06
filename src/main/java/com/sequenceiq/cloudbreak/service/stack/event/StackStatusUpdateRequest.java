package com.sequenceiq.cloudbreak.service.stack.event;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.StatusRequest;
import com.sequenceiq.cloudbreak.domain.User;

public class StackStatusUpdateRequest {

    private Long stackId;
    private CloudPlatform cloudPlatform;
    private StatusRequest statusRequest;
    private User user;

    public StackStatusUpdateRequest(User user, CloudPlatform cloudPlatform, Long stackId, StatusRequest statusRequest) {
        this.user = user;
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

    public CloudPlatform getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(CloudPlatform cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public StatusRequest getStatusRequest() {
        return statusRequest;
    }

    public void setStatusRequest(StatusRequest statusRequest) {
        this.statusRequest = statusRequest;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}