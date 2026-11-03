package com.sequenceiq.cloudbreak.reactor.api.event.stack;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class DatalakeReachabilityValidationResult extends StackEvent {

    @JsonCreator
    public DatalakeReachabilityValidationResult(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
