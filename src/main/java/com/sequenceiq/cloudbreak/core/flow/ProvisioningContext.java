package com.sequenceiq.cloudbreak.core.flow;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;

public class ProvisioningContext {
    private CloudPlatform cloudPlatform;
    private Long stackId;

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
