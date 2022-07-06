package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.service.stack.repair.UnhealthyInstances;

public class StackRepairTriggerEvent extends StackEvent {

    private final UnhealthyInstances unhealthyInstances;

    @JsonCreator
    public StackRepairTriggerEvent(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("unhealthyInstances") UnhealthyInstances unhealthyInstances) {
        super(stackId);
        this.unhealthyInstances = unhealthyInstances;
    }

    public UnhealthyInstances getUnhealthyInstances() {
        return unhealthyInstances;
    }
}
