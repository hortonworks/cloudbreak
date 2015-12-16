package com.sequenceiq.cloudbreak.core.flow.context;

import com.sequenceiq.cloudbreak.cloud.model.Platform;

public class DefaultFlowContext implements FlowContext {
    private Long stackId;
    private Platform cloudPlatform;
    private String errorReason;

    public DefaultFlowContext(Long stackId, Platform cloudPlatform) {
        this.stackId = stackId;
        this.cloudPlatform = cloudPlatform;
    }

    public DefaultFlowContext(Long stackId, Platform cloudPlatform, String errorReason) {
        this.stackId = stackId;
        this.cloudPlatform = cloudPlatform;
        this.errorReason = errorReason;
    }

    @Override
    public Long getStackId() {
        return stackId;
    }

    @Override
    public Platform getCloudPlatform() {
        return cloudPlatform;
    }

    public String getErrorReason() {
        return errorReason;
    }

    public void setErrorReason(String errorReason) {
        this.errorReason = errorReason;
    }
}
