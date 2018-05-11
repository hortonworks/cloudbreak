package com.sequenceiq.cloudbreak.service;

import com.sequenceiq.cloudbreak.domain.stack.Stack;

public abstract class StackContext {

    private final Stack stack;

    protected StackContext(Stack stack) {
        this.stack = stack;
    }

    public Stack getStack() {
        return stack;
    }
}
