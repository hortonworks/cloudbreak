package com.sequenceiq.cloudbreak.core.flow2.stack.repair;

import com.sequenceiq.cloudbreak.core.flow2.AbstractAction;
import com.sequenceiq.cloudbreak.core.flow2.FlowState;

public enum ManualStackRepairTriggerState implements FlowState {
    INIT_STATE,
    UNHEALTHY_INSTANCES_DETECTION_STATE,
    NOTIFY_STACK_REPAIR_SERVICE_STATE,
    MANUAL_STACK_REPAIR_TRIGGER_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends AbstractAction> action() {
        return null;
    }
}
