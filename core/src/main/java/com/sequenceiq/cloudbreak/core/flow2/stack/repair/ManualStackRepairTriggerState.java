package com.sequenceiq.cloudbreak.core.flow2.stack.repair;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum ManualStackRepairTriggerState implements FlowState {
    INIT_STATE,
    UNHEALTHY_INSTANCES_DETECTION_STATE,
    NOTIFY_STACK_REPAIR_SERVICE_STATE,
    MANUAL_STACK_REPAIR_TRIGGER_FAILED_STATE,
    FINAL_STATE;

    private final Class<? extends RestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }
}
