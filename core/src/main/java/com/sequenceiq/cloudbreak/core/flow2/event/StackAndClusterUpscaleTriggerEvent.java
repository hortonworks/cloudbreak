package com.sequenceiq.cloudbreak.core.flow2.event;

import com.sequenceiq.cloudbreak.common.type.ScalingType;

import java.util.Collections;
import java.util.Set;

public class StackAndClusterUpscaleTriggerEvent extends StackScaleTriggerEvent {
    private final ScalingType scalingType;

    public StackAndClusterUpscaleTriggerEvent(String selector, Long stackId, String instanceGroup, Integer adjustment, ScalingType scalingType) {
        this(selector, stackId, instanceGroup, adjustment, scalingType, Collections.emptySet());
    }

    public StackAndClusterUpscaleTriggerEvent(String selector, Long stackId,
            String instanceGroup, Integer adjustment, ScalingType scalingType, Set<String> hostNames) {
        super(selector, stackId, instanceGroup, adjustment, hostNames);
        this.scalingType = scalingType;
    }

    public ScalingType getScalingType() {
        return scalingType;
    }
}
