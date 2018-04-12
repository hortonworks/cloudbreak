package com.sequenceiq.cloudbreak.core.flow2.event;

import com.sequenceiq.cloudbreak.common.type.ScalingType;
import reactor.rx.Promise;

import java.util.Set;

public class ClusterAndStackDownscaleTriggerEvent extends ClusterDownscaleTriggerEvent {
    private final ScalingType scalingType;

    public ClusterAndStackDownscaleTriggerEvent(String selector, Long stackId, String hostGroup, Integer adjustment, ScalingType scalingType) {
        super(selector, stackId, hostGroup, adjustment);
        this.scalingType = scalingType;
    }

    public ClusterAndStackDownscaleTriggerEvent(String selector, Long stackId, String hostGroup, Set<String> hostNames, ScalingType scalingType,
            Promise<Boolean> accepted) {
        super(selector, stackId, hostGroup, hostNames, accepted);
        this.scalingType = scalingType;
    }

    public ScalingType getScalingType() {
        return scalingType;
    }
}
