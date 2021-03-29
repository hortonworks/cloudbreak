package com.sequenceiq.flow.core.chain.finalize.config;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

public enum FlowChainFinalizeState implements FlowState {
    INIT_STATE,
    FLOWCHAIN_FINALIZE_FINISHED_STATE,
    FLOWCHAIN_FINALIZE_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return DefaultRestartAction.class;
    }
}
