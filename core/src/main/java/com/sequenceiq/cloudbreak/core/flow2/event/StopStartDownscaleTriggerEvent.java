package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.Set;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class StopStartDownscaleTriggerEvent extends StackEvent {

    private final String hostGroup;

    private final Set<Long> hostIds;

    public StopStartDownscaleTriggerEvent(String selector, Long stackId, String hostGroup, Set<Long> hostIds) {
        super(selector, stackId);
        this.hostGroup = hostGroup;
        this.hostIds = hostIds;
    }

    public String getHostGroupName() {
        return hostGroup;
    }

    public Set<Long> getHostIds() {
        return hostIds;
    }

    @Override
    public String toString() {
        return "StopStartDownscaleTriggerEvent{" +
                "hostGroup='" + hostGroup + '\'' +
                ", hostIds=" + hostIds +
                '}';
    }
}
