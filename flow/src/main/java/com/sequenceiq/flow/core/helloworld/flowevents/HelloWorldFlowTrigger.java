package com.sequenceiq.flow.core.helloworld.flowevents;

import static com.sequenceiq.flow.core.helloworld.config.HelloWorldEvent.HELLOWORLD_TRIGGER_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.flow.core.helloworld.HelloWorldSelectableEvent;

public class HelloWorldFlowTrigger extends HelloWorldSelectableEvent {

    @JsonCreator
    public HelloWorldFlowTrigger(@JsonProperty("resourceId") Long resourceId) {
        super(resourceId);
    }

    @Override
    public String selector() {
        return HELLOWORLD_TRIGGER_EVENT.event();
    }

}
