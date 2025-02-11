package com.sequenceiq.datalake.flow.enableselinux;

import com.sequenceiq.datalake.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum DatalakeEnableSeLinuxState implements FlowState {

    INIT_STATE,
    ENABLE_SELINUX_DATALAKE_VALIDATION_STATE,
    ENABLE_SELINUX_DATALAKE_STATE,
    ENABLE_SELINUX_DATALAKE_FINISHED_STATE,
    ENABLE_SELINUX_DATALAKE_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
