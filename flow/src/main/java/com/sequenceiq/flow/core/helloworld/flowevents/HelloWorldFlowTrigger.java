package com.sequenceiq.flow.core.helloworld.flowevents;

import static com.sequenceiq.flow.core.helloworld.config.HelloWorldEvent.HELLOWORLD_TRIGGER_EVENT;

import com.sequenceiq.flow.core.helloworld.HelloWorldSelectableEvent;

public class HelloWorldFlowTrigger extends HelloWorldSelectableEvent {

    public HelloWorldFlowTrigger(Long resourceId) {
        super(resourceId);
    }

    @Override
    public String selector() {
        return HELLOWORLD_TRIGGER_EVENT.event();
    }

}
