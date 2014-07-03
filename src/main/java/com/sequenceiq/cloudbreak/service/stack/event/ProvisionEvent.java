package com.sequenceiq.cloudbreak.service.stack.event;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;

public class ProvisionEvent {

    private CloudPlatform cloudPlatform;
    private Long stackId;

    public ProvisionEvent(CloudPlatform cloudPlatform, Long stackId) {
        this.cloudPlatform = cloudPlatform;
        this.stackId = stackId;
    }

    public CloudPlatform getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(CloudPlatform cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }

}
