package com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum CoreEnableSeLinuxState implements FlowState {

    INIT_STATE,
    ENABLE_SELINUX_CORE_VALIDATION_STATE,
    ENABLE_SELINUX_CORE_STATE,
    ENABLE_SELINUX_CORE_FINISHED_STATE,
    ENABLE_SELINUX_CORE_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
