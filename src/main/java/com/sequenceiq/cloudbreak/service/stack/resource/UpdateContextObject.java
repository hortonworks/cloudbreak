package com.sequenceiq.cloudbreak.service.stack.resource;

import com.sequenceiq.cloudbreak.domain.Stack;

public abstract class UpdateContextObject {

    private Stack stack;

    public UpdateContextObject(Stack stack) {
        this.stack = stack;
    }

    public Stack getStack() {
        return stack;
    }

    public void setStack(Stack stack) {
        this.stack = stack;
    }
}
