package com.sequenceiq.cloudbreak.reactor.api.event.stack;

import java.util.Objects;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

import reactor.rx.Promise;

public class TerminationEvent extends StackEvent {

    private final TerminationType terminationType;

    public TerminationEvent(String selector, Long stackId, TerminationType terminationType) {
        super(selector, stackId, new Promise<>());
        this.terminationType = terminationType;
    }

    public TerminationEvent(String selector, Long stackId, TerminationType terminationType, Promise<AcceptResult> accepted) {
        super(selector, stackId, accepted);
        this.terminationType = terminationType;
    }

    public TerminationType getTerminationType() {
        return Objects.nonNull(terminationType) ? terminationType : TerminationType.REGULAR;
    }
}
