package com.sequenceiq.cloudbreak.core.flow2.stack.repair;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.service.stack.repair.UnhealthyInstances;

public class StackRepairTriggerEvent extends StackEvent {

    private final UnhealthyInstances unhealthyInstances;

    public StackRepairTriggerEvent(Long stackId, UnhealthyInstances unhealthyInstances) {
        super(stackId);
        this.unhealthyInstances = unhealthyInstances;
    }

    public UnhealthyInstances getUnhealthyInstances() {
        return unhealthyInstances;
    }
}
