package com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class RestartCmForLbSuccess extends StackEvent {
    @JsonCreator
    public RestartCmForLbSuccess(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
