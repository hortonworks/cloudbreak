package com.sequenceiq.cloudbreak.reactor.api.event.stack;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class TerminationEvent extends StackEvent {

    private final TerminationType terminationType;

    public TerminationEvent(String selector, Long stackId, TerminationType terminationType) {
        super(selector, stackId, new Promise<>());
        this.terminationType = terminationType;
    }

    @JsonCreator
    public TerminationEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("terminationType") TerminationType terminationType,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted) {
        super(selector, stackId, accepted);
        this.terminationType = terminationType;
    }

    public TerminationType getTerminationType() {
        return terminationType;
    }

    @Override
    public String toString() {
        return "TerminationEvent{" +
                "terminationType=" + terminationType +
                "} " + super.toString();
    }
}
