package com.sequenceiq.freeipa.flow.stack.termination.event;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

import reactor.rx.Promise;

public class TerminationEvent extends StackEvent {

    private final Boolean forced;

    @JsonCreator
    public TerminationEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("forced") Boolean forced) {
        super(selector, stackId, new Promise<>());
        this.forced = forced;
    }

    public Boolean getForced() {
        return forced;
    }

    @Override
    public boolean equalsEvent(StackEvent other) {
        return isClassAndEqualsEvent(TerminationEvent.class, other,
                event -> Objects.equals(forced, event.forced));
    }

    @Override
    public String toString() {
        return "TerminationEvent{" +
                "forced=" + forced +
                "} " + super.toString();
    }

}
