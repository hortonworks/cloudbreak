package com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class CreateLoadBalancerEntitySuccess extends StackEvent {

    private final Stack savedStack;

    public CreateLoadBalancerEntitySuccess(Long stackId, Stack savedStack) {
        super(stackId);
        this.savedStack = savedStack;
    }

    public Stack getSavedStack() {
        return savedStack;
    }
}
