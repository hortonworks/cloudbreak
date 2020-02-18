package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.Set;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.type.ScalingType;

import reactor.rx.Promise;

public class ClusterAndStackDownscaleTriggerEvent extends ClusterDownscaleTriggerEvent {
    private final ScalingType scalingType;

    public ClusterAndStackDownscaleTriggerEvent(String selector, Long stackId, String hostGroup, Integer adjustment, ScalingType scalingType) {
        super(selector, stackId, hostGroup, adjustment);
        this.scalingType = scalingType;
    }

    public ClusterAndStackDownscaleTriggerEvent(String selector, Long stackId, String hostGroup, Set<Long> privateIds, ScalingType scalingType,
            Promise<AcceptResult> accepted, ClusterDownscaleDetails details) {
        super(selector, stackId, hostGroup, privateIds, accepted, details);
        this.scalingType = scalingType;
    }

    public ScalingType getScalingType() {
        return scalingType;
    }
}
