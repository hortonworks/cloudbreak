package com.sequenceiq.flow.core.helloworld;

import com.sequenceiq.flow.core.FlowEvent;

public enum HelloWorldEvent implements FlowEvent {
    START_HELLO_WORLD_EVENT,
    HELLO_WORLD_FINISHED_EVENT,
    FINALIZE_HELLO_WORLD_EVENT,

    HELLO_WORLD_SOMETHING_WENT_WRONG,
    HELLO_WORLD_FAIL_HANDLED_EVENT;

    @Override
    public String event() {
        return name();
    }
}
