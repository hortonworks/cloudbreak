package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.common.type.ScalingType;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

import reactor.rx.Promise;

public class MultiHostgroupClusterAndStackDownscaleTriggerEvent extends StackEvent {
    private final ScalingType scalingType;

    private final Map<String, Set<Long>> instanceIdsByHostgroupMap;

    public MultiHostgroupClusterAndStackDownscaleTriggerEvent(String selector, Long stackId, Map<String, Set<Long>> instanceIdsByHostgroupMap,
            ScalingType scalingType, Promise<Boolean> accepted) {
        super(selector, stackId, accepted);
        this.instanceIdsByHostgroupMap = instanceIdsByHostgroupMap;
        this.scalingType = scalingType;
    }

    public ScalingType getScalingType() {
        return scalingType;
    }

    public Map<String, Set<Long>> getInstanceIdsByHostgroupMap() {
        return instanceIdsByHostgroupMap;
    }
}
