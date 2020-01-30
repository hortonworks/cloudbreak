package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.type.ScalingType;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

import reactor.rx.Promise;

public class MultiHostgroupClusterAndStackDownscaleTriggerEvent extends StackEvent {
    private final ScalingType scalingType;

    private final Map<String, Set<Long>> instanceIdsByHostgroupMap;

    private final ClusterDownscaleDetails details;

    public MultiHostgroupClusterAndStackDownscaleTriggerEvent(String selector, Long stackId, Map<String, Set<Long>> instanceIdsByHostgroupMap,
            ClusterDownscaleDetails details, ScalingType scalingType, Promise<AcceptResult> accepted) {
        super(selector, stackId, accepted);
        this.instanceIdsByHostgroupMap = instanceIdsByHostgroupMap;
        this.details = details;
        this.scalingType = scalingType;
    }

    public ScalingType getScalingType() {
        return scalingType;
    }

    public Map<String, Set<Long>> getInstanceIdsByHostgroupMap() {
        return instanceIdsByHostgroupMap;
    }

    public ClusterDownscaleDetails getDetails() {
        return details;
    }
}
