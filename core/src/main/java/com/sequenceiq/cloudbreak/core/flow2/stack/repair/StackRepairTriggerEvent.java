package com.sequenceiq.cloudbreak.core.flow2.stack.repair;

import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.service.stack.repair.UnhealthyInstances;

public class StackRepairTriggerEvent implements Payload {

    private final Long stackId;

    private final UnhealthyInstances unhealthyInstances;

    public StackRepairTriggerEvent(Long stackId, UnhealthyInstances unhealthyInstances) {
        this.stackId = stackId;
        this.unhealthyInstances = unhealthyInstances;
    }

    public Long getStackId() {
        return stackId;
    }

    public UnhealthyInstances getUnhealthyInstances() {
        return unhealthyInstances;
    }
}
