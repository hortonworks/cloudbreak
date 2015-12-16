package com.sequenceiq.cloudbreak.service.stack.event;

import com.sequenceiq.cloudbreak.cloud.model.Platform;

public class StackDeleteRequest {

    private Long stackId;
    private Platform cloudPlatform;

    public StackDeleteRequest(Platform cloudPlatform, Long stackId) {
        this.stackId = stackId;
        this.cloudPlatform = cloudPlatform;
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
}
