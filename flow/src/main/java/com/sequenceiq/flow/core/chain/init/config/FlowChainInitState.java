package com.sequenceiq.flow.core.chain.init.config;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

public enum FlowChainInitState implements FlowState {
    INIT_STATE,
    FLOWCHAIN_INIT_FINISHED_STATE,
    FLOWCHAIN_INIT_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return DefaultRestartAction.class;
    }
}
