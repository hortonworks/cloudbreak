package com.sequenceiq.cloudbreak.core.flow2.cluster.modifyselinux;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum CoreModifySeLinuxState implements FlowState {

    INIT_STATE,
    MODIFY_SELINUX_CORE_VALIDATION_STATE,
    MODIFY_SELINUX_CORE_STATE,
    MODIFY_SELINUX_CORE_FINISHED_STATE,
    MODIFY_SELINUX_CORE_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
