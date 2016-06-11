package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleRequest;

public class UpscaleAmbariRequest extends AbstractClusterScaleRequest {

    private final Integer scalingAdjustment;

    public UpscaleAmbariRequest(Long stackId, String hostGroupName, Integer scalingAdjustment) {
        super(stackId, hostGroupName);
        this.scalingAdjustment = scalingAdjustment;
    }

    public Integer getScalingAdjustment() {
        return scalingAdjustment;
    }
}
