package com.sequenceiq.flow.core.chain.init.config;

import com.sequenceiq.flow.core.FlowState;

public enum FlowChainInitState implements FlowState {
    INIT_STATE,
    FLOWCHAIN_INIT_FINISHED_STATE,
    FLOWCHAIN_INIT_FAILED_STATE,
    FINAL_STATE
}
