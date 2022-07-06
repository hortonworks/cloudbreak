package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class StopStartDownscaleTriggerEvent extends StackEvent {

    private final String hostGroup;

    private final Set<Long> hostIds;

    @JsonCreator
    public StopStartDownscaleTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("hostGroup") String hostGroup,
            @JsonProperty("hostIds") Set<Long> hostIds) {
        super(selector, stackId);
        this.hostGroup = hostGroup;
        this.hostIds = hostIds;
    }

    public String getHostGroup() {
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
