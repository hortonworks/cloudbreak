package com.sequenceiq.cloudbreak.reactor.api.event.stack;

import java.util.Objects;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

import reactor.rx.Promise;

public class TerminationEvent extends StackEvent {

    // TODO: this is there for backward compatibility,
    //  should be removed if it is guaranteed that
    //  there is no running termination flow using it
    //  Tracked here: CB-13743
    private final Boolean forced;

    private final TerminationType terminationType;

    public TerminationEvent(String selector, Long stackId, Boolean forced) {
        super(selector, stackId, new Promise<>());
        this.forced = forced;
        this.terminationType = getTerminationType();
    }

    public TerminationEvent(String selector, Long stackId, Boolean forced, Promise<AcceptResult> accepted) {
        super(selector, stackId, accepted);
        this.forced = forced;
        this.terminationType = getTerminationType();
    }

    public TerminationEvent(String selector, Long stackId, TerminationType terminationType) {
        super(selector, stackId, new Promise<>());
        this.terminationType = terminationType;
        this.forced = terminationType.isForced();
    }

    public TerminationEvent(String selector, Long stackId, TerminationType terminationType, Promise<AcceptResult> accepted) {
        super(selector, stackId, accepted);
        this.terminationType = terminationType;
        this.forced = terminationType.isForced();
    }

    public TerminationType getTerminationType() {
        return
                Objects.nonNull(terminationType)
                        ? terminationType
                        : getForced();
    }

    private TerminationType getForced() {
        return Boolean.TRUE.equals(forced) ? TerminationType.FORCED : TerminationType.REGULAR;
    }

    @Override
    public String toString() {
        return "TerminationEvent{" +
                "forced=" + forced +
                ", terminationType=" + terminationType +
                "} " + super.toString();
    }
}
