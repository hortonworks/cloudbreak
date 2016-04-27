package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale;

import com.sequenceiq.cloudbreak.reactor.api.event.ClusterUpscalePayload;

public class UpscaleClusterFailedPayload implements ClusterUpscalePayload {

    private final String cloudPlatform;
    private final Long stackId;
    private final String hostGroupName;
    private final Integer scalingAdjustment;
    private final String errorReason;

    public UpscaleClusterFailedPayload(String cloudPlatform, Long stackId, String hostGroupName, Integer scalingAdjustment, String errorReason) {
        this.cloudPlatform = cloudPlatform;
        this.stackId = stackId;
        this.hostGroupName = hostGroupName;
        this.scalingAdjustment = scalingAdjustment;
        this.errorReason = errorReason;
    }

    @Override
    public Long getStackId() {
        return stackId;
    }

    @Override
    public String getCloudPlatformName() {
        return cloudPlatform;
    }

    @Override
    public String getHostGroupName() {
        return hostGroupName;
    }

    @Override
    public Integer getScalingAdjustment() {
        return scalingAdjustment;
    }

    public String getErrorReason() {
        return errorReason;
    }
}
