package com.sequenceiq.flow.core.chain.finalize.config;

import com.sequenceiq.flow.core.FlowState;

public enum FlowChainFinalizeState implements FlowState {
    INIT_STATE,
    FLOWCHAIN_FINALIZE_FINISHED_STATE,
    FLOWCHAIN_FINALIZE_FAILED_STATE,
    FINAL_STATE
}
