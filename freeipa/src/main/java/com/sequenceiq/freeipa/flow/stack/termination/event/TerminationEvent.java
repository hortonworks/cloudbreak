package com.sequenceiq.freeipa.flow.stack.termination.event;

import com.sequenceiq.freeipa.flow.stack.StackEvent;

import reactor.rx.Promise;

public class TerminationEvent extends StackEvent {

    private final Boolean forced;

    public TerminationEvent(String selector, Long stackId, Boolean forced) {
        super(selector, stackId, new Promise<>());
        this.forced = forced;
    }

    public Boolean getForced() {
        return forced;
    }
}
