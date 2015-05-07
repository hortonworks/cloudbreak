package com.sequenceiq.cloudbreak.service;

import com.sequenceiq.cloudbreak.domain.Stack;

public abstract class StackContext {

    private Stack stack;

    public StackContext(Stack stack) {
        this.stack = stack;
    }

    public Stack getStack() {
        return stack;
    }
}
