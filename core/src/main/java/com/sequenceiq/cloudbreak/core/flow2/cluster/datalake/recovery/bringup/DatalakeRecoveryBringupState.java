package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.recovery.bringup;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum DatalakeRecoveryBringupState implements FlowState {
    INIT_STATE,
    RECOVERY_SETUP_NEW_INSTANCES_STATE,
    RECOVERY_BRINGUP_FAILED_STATE,
    RECOVERY_BRINGUP_FINISHED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
