package com.sequenceiq.cloudbreak.reactor.api.event.resource;

public class DecommissionRequest extends AbstractClusterScaleRequest {

    private final Integer scalingAdjustment;

    public DecommissionRequest(Long stackId, String hostGroupName, Integer scalingAdjustment) {
        super(stackId, hostGroupName);
        this.scalingAdjustment = scalingAdjustment;
    }

    public Integer getScalingAdjustment() {
        return scalingAdjustment;
    }
}
