package com.sequenceiq.flow.core.helloworld.config;

import com.sequenceiq.flow.core.FlowEvent;

public enum HelloWorldEvent implements FlowEvent {
    HELLOWORLD_TRIGGER_EVENT,
    HELLOWORLD_FIRST_STEP_FINISHED_EVENT,
    HELLOWORLD_FIRST_STEP_WENT_WRONG_EVENT,
    HELLOWORLD_SECOND_STEP_FINISHED_EVENT,
    FINALIZE_HELLOWORLD_EVENT,
    HELLOWORLD_SOMETHING_WENT_WRONG,
    HELLOWORLD_FAILHANDLED_EVENT;

    private String selector;

    HelloWorldEvent() {
        this.selector = name();
    }

    @Override
    public String event() {
        return selector;
    }
}
