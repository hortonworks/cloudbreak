package com.sequenceiq.datalake.flow.modifyselinux;

import com.sequenceiq.datalake.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum DatalakeModifySeLinuxState implements FlowState {

    INIT_STATE,
    MODIFY_SELINUX_DATALAKE_STATE,
    MODIFY_SELINUX_DATALAKE_FINISHED_STATE,
    MODIFY_SELINUX_DATALAKE_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
