package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.ClusterUpscalePayload;

abstract class AbstractClusterUpscaleRequest extends ClusterPlatformRequest implements ClusterUpscalePayload {
    private final String cloudPlatformName;
    private final String hostGroupName;
    private final Integer scalingAdjustment;

    AbstractClusterUpscaleRequest(Long stackId, String cloudPlatformName, String hostGroupName, Integer scalingAdjustment) {
        super(stackId);
        this.cloudPlatformName = cloudPlatformName;
        this.hostGroupName = hostGroupName;
        this.scalingAdjustment = scalingAdjustment;
    }

    @Override
    public String getCloudPlatformName() {
        return cloudPlatformName;
    }

    @Override
    public String getHostGroupName() {
        return hostGroupName;
    }

    @Override
    public Integer getScalingAdjustment() {
        return scalingAdjustment;
    }
}
