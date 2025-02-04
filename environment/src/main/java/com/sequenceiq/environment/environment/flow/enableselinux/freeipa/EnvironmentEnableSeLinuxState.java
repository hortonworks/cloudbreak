package com.sequenceiq.environment.environment.flow.enableselinux.freeipa;

import com.sequenceiq.environment.environment.flow.EnvironmentFillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum EnvironmentEnableSeLinuxState implements FlowState {

    INIT_STATE,
    ENABLE_SELINUX_FREEIPA_VALIDATION_STATE,
    ENABLE_SELINUX_FREEIPA_STATE,
    ENABLE_SELINUX_FREEIPA_FINISHED_STATE,
    ENABLE_SELINUX_FREEIPA_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return EnvironmentFillInMemoryStateStoreRestartAction.class;
    }
}
