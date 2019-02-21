package com.sequenceiq.cloudbreak.service;

import com.sequenceiq.cloudbreak.cluster.service.StackAware;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

public abstract class StackContext implements StackAware {

    private final Stack stack;

    protected StackContext(Stack stack) {
        this.stack = stack;
    }

    public Stack getStack() {
        return stack;
    }
}
