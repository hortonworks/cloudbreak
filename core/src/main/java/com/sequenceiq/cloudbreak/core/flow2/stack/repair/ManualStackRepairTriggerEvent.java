package com.sequenceiq.cloudbreak.core.flow2.stack.repair;

import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;
import com.sequenceiq.cloudbreak.core.flow2.FlowTriggers;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.UnhealthyInstancesDetectionResult;

public enum ManualStackRepairTriggerEvent implements FlowEvent {
    MANUAL_STACK_REPAIR_TRIGGER_EVENT(FlowTriggers.MANUAL_STACK_REPAIR_TRIGGER_EVENT),
    NOTIFY_REPAIR_SERVICE_EVENT(EventSelectorUtil.selector(UnhealthyInstancesDetectionResult.class)),
    REPAIR_SERVICE_NOTIFIED_EVENT("REPAIR_SERVICE_NOTIFIED_EVENT"),
    MANUAL_STACK_REPAIR_TRIGGER_FAILURE_EVENT("MANUAL_STACK_REPAIR_TRIGGER_FAILURE_EVENT"),
    MANUAL_STACK_REPAIR_TRIGGER_FAILURE_HANDLED_EVENT("MANUAL_STACK_REPAIR_TRIGGER_FAILURE_HANDLED_EVENT");

    private String stringRepresentation;

    ManualStackRepairTriggerEvent(String stringRepresentation) {
        this.stringRepresentation = stringRepresentation;
    }

    @Override
    public String stringRepresentation() {
        return stringRepresentation;
    }
}
