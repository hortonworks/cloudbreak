package com.sequenceiq.cloudbreak.service.stack.event;

import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.cloud.model.Platform;

public class ProvisionEvent implements Payload {

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

    @Override
    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }

}
