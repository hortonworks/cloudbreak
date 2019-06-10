package com.sequenceiq.flow.core.helloworld.flowevents;

import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.flow.core.helloworld.HelloWorldSelectableEvent;

import reactor.rx.Promise;

public class HelloWorldFlowTriggerEvent extends HelloWorldSelectableEvent implements Acceptable {
    private Promise<Boolean> accepted;

    public HelloWorldFlowTriggerEvent(Long resourceId, Promise<Boolean> accepted) {
        super(resourceId);
        this.accepted = accepted;
    }

    @Override
    public Promise<Boolean> accepted() {
        return accepted;
    }
}
