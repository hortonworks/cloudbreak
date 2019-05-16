package com.sequenceiq.cloudbreak.core.flow2.stack.repair;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.UnhealthyInstancesDetectionResult;

public enum ManualStackRepairTriggerEvent implements FlowEvent {
    MANUAL_STACK_REPAIR_TRIGGER_EVENT("MANUAL_STACK_REPAIR_TRIGGER_EVENT"),
    NOTIFY_REPAIR_SERVICE_EVENT(EventSelectorUtil.selector(UnhealthyInstancesDetectionResult.class)),
    REPAIR_SERVICE_NOTIFIED_EVENT("REPAIR_SERVICE_NOTIFIED_EVENT"),
    MANUAL_STACK_REPAIR_TRIGGER_FAILURE_EVENT(EventSelectorUtil.failureSelector(UnhealthyInstancesDetectionResult.class)),
    MANUAL_STACK_REPAIR_TRIGGER_FAILURE_HANDLED_EVENT("MANUAL_STACK_REPAIR_TRIGGER_FAILURE_HANDLED_EVENT");

    private final String event;

    ManualStackRepairTriggerEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
