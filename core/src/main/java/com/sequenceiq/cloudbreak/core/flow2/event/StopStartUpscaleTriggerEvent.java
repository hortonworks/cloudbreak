package com.sequenceiq.cloudbreak.core.flow2.event;

import com.sequenceiq.cloudbreak.common.type.ClusterManagerType;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class StopStartUpscaleTriggerEvent extends StackEvent {

    private final String hostGroup;

    private final Integer adjustment;

    public StopStartUpscaleTriggerEvent(String selector, Long stackId, String hostGroup, Integer adjustment, ClusterManagerType clusterManagerType) {
        super(selector, stackId);
        this.hostGroup = hostGroup;
        this.adjustment = adjustment;
    }

    public String getHostGroupName() {
        return hostGroup;
    }

    public Integer getAdjustment() {
        return adjustment;
    }

    @Override
    public String toString() {
        return "StopStartUpscaleTriggerEvent{" +
                "hostGroup='" + hostGroup + '\'' +
                ", adjustment=" + adjustment +
                '}';
    }
}
