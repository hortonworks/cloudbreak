package com.sequenceiq.freeipa.flow.freeipa.enableselinux;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.freeipa.flow.FillInMemoryStateStoreRestartAction;

public enum FreeIpaEnableSeLinuxState implements FlowState {

    INIT_STATE,
    ENABLE_SELINUX_FREEIPA_VALIDATION_STATE,
    ENABLE_SELINUX_FREEIPA_STATE,
    ENABLE_SELINUX_FREEIPA_FINISHED_STATE,
    ENABLE_SELINUX_FREEIPA_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
