package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.type.ScalingType;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

import reactor.rx.Promise;

public class MultiHostgroupClusterAndStackDownscaleTriggerEvent extends StackEvent {
    private final ScalingType scalingType;

    private final Map<String, Set<Long>> privateIdsByHostgroupMap;

    private final ClusterDownscaleDetails details;

    public MultiHostgroupClusterAndStackDownscaleTriggerEvent(String selector, Long stackId, Map<String, Set<Long>> privateIdsByHostgroupMap,
            ClusterDownscaleDetails details, ScalingType scalingType, Promise<AcceptResult> accepted) {
        super(selector, stackId, accepted);
        this.privateIdsByHostgroupMap = privateIdsByHostgroupMap;
        this.details = details;
        this.scalingType = scalingType;
    }

    public ScalingType getScalingType() {
        return scalingType;
    }

    public Map<String, Set<Long>> getPrivateIdsByHostgroupMap() {
        return privateIdsByHostgroupMap;
    }

    public ClusterDownscaleDetails getDetails() {
        return details;
    }
}
