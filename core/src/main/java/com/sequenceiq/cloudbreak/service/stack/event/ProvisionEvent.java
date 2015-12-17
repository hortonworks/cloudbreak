package com.sequenceiq.cloudbreak.service.stack.event;

import com.sequenceiq.cloudbreak.cloud.model.Platform;

public class ProvisionEvent {

    private Platform cloudPlatform;
    private Long stackId;

    public ProvisionEvent() {
    }

    public ProvisionEvent(Platform cloudPlatform, Long stackId) {
        this.cloudPlatform = cloudPlatform;
        this.stackId = stackId;
    }

    public Platform getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(Platform cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }

}
