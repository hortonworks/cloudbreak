package com.sequenceiq.cloudbreak.core.flow.context;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;

public class TerminationContext implements FlowContext {
    private Long stackId;
    private CloudPlatform cloudPlatform;
    private String statusReason;

    public TerminationContext(Long stackId, CloudPlatform cloudPlatform) {
        this.stackId = stackId;
        this.cloudPlatform = cloudPlatform;
    }

    public TerminationContext(Long stackId, CloudPlatform cloudPlatform, String statusReason) {
        this.stackId = stackId;
        this.cloudPlatform = cloudPlatform;
        this.statusReason = statusReason;
    }

    public Long getStackId() {
        return stackId;
    }

    public CloudPlatform getCloudPlatform() {
        return cloudPlatform;
    }

    public String getStatusReason() {
        return statusReason;
    }
}
