package com.sequenceiq.cloudbreak.core.flow2.event;

import com.sequenceiq.cloudbreak.common.type.ScalingType;

public class StackAndClusterUpscaleTriggerEvent extends StackScaleTriggerEvent {
    private final ScalingType scalingType;

    public StackAndClusterUpscaleTriggerEvent(String selector, Long stackId, String instanceGroup, Integer adjustment, ScalingType scalingType) {
        super(selector, stackId, instanceGroup, adjustment);
        this.scalingType = scalingType;
    }

    public ScalingType getScalingType() {
        return scalingType;
    }
}
