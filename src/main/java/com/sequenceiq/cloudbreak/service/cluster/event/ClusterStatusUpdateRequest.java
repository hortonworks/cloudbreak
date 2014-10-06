package com.sequenceiq.cloudbreak.service.cluster.event;

import com.sequenceiq.cloudbreak.domain.StatusRequest;
import com.sequenceiq.cloudbreak.domain.User;

public class ClusterStatusUpdateRequest {

    private Long stackId;
    private StatusRequest statusRequest;
    private User user;

    public ClusterStatusUpdateRequest(User user, long stackId, StatusRequest statusRequest) {
        this.user = user;
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}