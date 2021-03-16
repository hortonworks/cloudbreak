package com.sequenceiq.flow.core.chain.finalize.config;

import com.sequenceiq.flow.core.FlowEvent;

public enum FlowChainFinalizeEvent implements FlowEvent {
    FLOWCHAIN_FINALIZE_TRIGGER_EVENT,
    FLOWCHAIN_FINALIZE_FINISHED_EVENT,
    FLOWCHAIN_FINALIZE_FAILED_EVENT,
    FLOWCHAIN_FINALIZE_FAILHANDLED_EVENT;

    private String selector;

    FlowChainFinalizeEvent() {
        this.selector = name();
    }

    @Override
    public String event() {
        return selector;
    }
}
