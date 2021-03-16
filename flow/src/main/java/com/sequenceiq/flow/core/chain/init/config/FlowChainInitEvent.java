package com.sequenceiq.flow.core.chain.init.config;

import com.sequenceiq.flow.core.FlowEvent;

public enum FlowChainInitEvent implements FlowEvent {
    FLOWCHAIN_INIT_TRIGGER_EVENT,
    FLOWCHAIN_INIT_FINISHED_EVENT,
    FLOWCHAIN_INIT_FAILED_EVENT,
    FLOWCHAIN_INIT_FAILHANDLED_EVENT;

    private String selector;

    FlowChainInitEvent() {
        this.selector = name();
    }

    @Override
    public String event() {
        return selector;
    }
}
