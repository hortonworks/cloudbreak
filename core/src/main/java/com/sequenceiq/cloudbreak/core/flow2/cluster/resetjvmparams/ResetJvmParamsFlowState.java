package com.sequenceiq.cloudbreak.core.flow2.cluster.resetjvmparams;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum ResetJvmParamsFlowState implements FlowState {

    INIT_STATE,
    RESET_JVM_PARAMS_STATE,
    RESET_JVM_PARAMS_FINISHED_STATE,
    FINAL_STATE,
    RESET_JVM_PARAMS_FAILED_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
