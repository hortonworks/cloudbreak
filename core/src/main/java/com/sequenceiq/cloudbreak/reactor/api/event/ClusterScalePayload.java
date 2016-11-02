package com.sequenceiq.cloudbreak.reactor.api.event;

public class ClusterScalePayload implements ScalingAdjustmentPayload {

    private final Long stackId;

    private final String hostGroupName;

    private final Integer scalingAdjustment;

    public ClusterScalePayload(Long stackId, String hostGroupName, Integer scalingAdjustment) {
        this.stackId = stackId;
        this.hostGroupName = hostGroupName;
        this.scalingAdjustment = scalingAdjustment;
    }

    @Override
    public Long getStackId() {
        return stackId;
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
