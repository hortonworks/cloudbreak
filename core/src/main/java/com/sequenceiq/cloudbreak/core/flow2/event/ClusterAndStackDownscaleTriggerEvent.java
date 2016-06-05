package com.sequenceiq.cloudbreak.core.flow2.event;

import com.sequenceiq.cloudbreak.common.type.ScalingType;

public class ClusterAndStackDownscaleTriggerEvent extends ClusterScaleTriggerEvent {
    private ScalingType scalingType;

    public ClusterAndStackDownscaleTriggerEvent(String selector, Long stackId, String hostGroup, Integer adjustment, ScalingType scalingType) {
        super(selector, stackId, hostGroup, adjustment);
        this.scalingType = scalingType;
    }

    public ScalingType getScalingType() {
        return scalingType;
    }
}
