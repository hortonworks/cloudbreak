package com.sequenceiq.flow.core.helloworld.config;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.helloworld.HelloWorldSelectableEvent;
import com.sequenceiq.flow.core.helloworld.flowevents.HelloWorldLongLastingTaskFailureResponse;
import com.sequenceiq.flow.core.helloworld.flowevents.HelloWorldLongLastingTaskSuccessResponse;
import com.sequenceiq.flow.core.helloworld.flowevents.HelloWorldFlowTriggerEvent;

public enum HelloWorldEvent implements FlowEvent {
    HELLOWORLD_TRIGGER_EVENT(HelloWorldSelectableEvent.selector(HelloWorldFlowTriggerEvent.class)),
    HELLOWORLD_FINISHED_EVENT(HelloWorldSelectableEvent.selector(HelloWorldLongLastingTaskSuccessResponse.class)),
    FINALIZE_HELLOWORLD_EVENT,
    HELLOWORLD_SOMETHING_WENT_WRONG(HelloWorldSelectableEvent.failureSelector(HelloWorldLongLastingTaskFailureResponse.class)),
    HELLOWORLD_FAILHANDLED_EVENT;

    private String selector;

    HelloWorldEvent() {
        this.selector = name();
    }

    HelloWorldEvent(String selector) {
        this.selector = selector;
    }

    @Override
    public String event() {
        return selector;
    }
}
