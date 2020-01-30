package com.sequenceiq.cloudbreak.reactor.api.event.stack;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

import reactor.rx.Promise;

public class TerminationEvent extends StackEvent {

    private final Boolean forced;

    public TerminationEvent(String selector, Long stackId, Boolean forced) {
        super(selector, stackId, new Promise<>());
        this.forced = forced;
    }

    public TerminationEvent(String selector, Long stackId, Boolean forced, Promise<AcceptResult> accepted) {
        super(selector, stackId, accepted);
        this.forced = forced;
    }

    public Boolean getForced() {
        return forced;
    }
}
