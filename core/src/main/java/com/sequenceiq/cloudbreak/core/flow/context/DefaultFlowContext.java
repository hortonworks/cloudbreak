package com.sequenceiq.cloudbreak.core.flow.context;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;

public class DefaultFlowContext implements FlowContext {
    private Long stackId;
    private CloudPlatform cloudPlatform;
    private String errorReason;

    public DefaultFlowContext(Long stackId, CloudPlatform cloudPlatform) {
        this.stackId = stackId;
        this.cloudPlatform = cloudPlatform;
    }

    public DefaultFlowContext(Long stackId, CloudPlatform cloudPlatform, String errorReason) {
        this.stackId = stackId;
        this.cloudPlatform = cloudPlatform;
        this.errorReason = errorReason;
    }

    public Long getStackId() {
        return stackId;
    }

    public CloudPlatform getCloudPlatform() {
        return cloudPlatform;
    }

    public String getErrorReason() {
        return errorReason;
    }

    public void setErrorReason(String errorReason) {
        this.errorReason = errorReason;
    }
}
