package com.sequenceiq.flow.core.helloworld.flowevents;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.flow.core.helloworld.HelloWorldSelectableEvent;

import reactor.rx.Promise;

public class HelloWorldFlowTriggerEvent extends HelloWorldSelectableEvent implements Acceptable {
    private Promise<AcceptResult> accepted;

    public HelloWorldFlowTriggerEvent(Long resourceId, Promise<AcceptResult> accepted) {
        super(resourceId);
        this.accepted = accepted;
    }

    @Override
    public Promise<AcceptResult> accepted() {
        return accepted;
    }
}
