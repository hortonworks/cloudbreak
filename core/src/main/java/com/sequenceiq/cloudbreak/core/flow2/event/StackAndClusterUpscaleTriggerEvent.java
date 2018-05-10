package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.Collections;
import java.util.Set;

import com.sequenceiq.cloudbreak.common.type.ScalingType;

public class StackAndClusterUpscaleTriggerEvent extends StackScaleTriggerEvent {

    private final ScalingType scalingType;

    private final Set<String> hostNames;

    public StackAndClusterUpscaleTriggerEvent(String selector, Long stackId, String instanceGroup, Integer adjustment, ScalingType scalingType) {
        this(selector, stackId, instanceGroup, adjustment, scalingType, Collections.emptySet());
    }

    public StackAndClusterUpscaleTriggerEvent(String selector, Long stackId,
            String instanceGroup, Integer adjustment, ScalingType scalingType, Set<String> hostNames) {
        super(selector, stackId, instanceGroup, adjustment);
        this.hostNames = hostNames;
        this.scalingType = scalingType;
    }

    public ScalingType getScalingType() {
        return scalingType;
    }

    public Set<String> getHostNames() {
        return hostNames;
    }
}
