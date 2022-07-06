package com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class CreateLoadBalancerEntitySuccess extends StackEvent {

    private final Stack savedStack;

    @JsonCreator
    public CreateLoadBalancerEntitySuccess(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("savedStack") Stack savedStack) {
        super(stackId);
        this.savedStack = savedStack;
    }

    public Stack getSavedStack() {
        return savedStack;
    }
}
