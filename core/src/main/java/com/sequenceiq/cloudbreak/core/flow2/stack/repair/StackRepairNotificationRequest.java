package com.sequenceiq.cloudbreak.core.flow2.stack.repair;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.domain.Stack;

import java.util.Set;

public class StackRepairNotificationRequest implements Selectable {

    private final Stack stack;

    private final Set<String> unhealthyInstanceIds;

    public StackRepairNotificationRequest(Stack stack, Set<String> unhealthyInstanceIds) {
        this.stack = stack;
        this.unhealthyInstanceIds = unhealthyInstanceIds;
    }

    @Override
    public String selector() {
        return ManualStackRepairTriggerEvent.NOTIFY_REPAIR_SERVICE_EVENT.stringRepresentation();
    }

    @Override
    public Long getStackId() {
        return stack.getId();
    }

    public Stack getStack() {
        return stack;
    }

    public Set<String> getUnhealthyInstanceIds() {
        return unhealthyInstanceIds;
    }
}
