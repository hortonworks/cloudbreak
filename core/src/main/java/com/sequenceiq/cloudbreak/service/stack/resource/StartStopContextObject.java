package com.sequenceiq.cloudbreak.service.stack.resource;

import com.sequenceiq.cloudbreak.domain.Stack;

public abstract class StartStopContextObject {

    private Stack stack;

    protected StartStopContextObject(Stack stack) {
        this.stack = stack;
    }

    public Stack getStack() {
        return stack;
    }

    public void setStack(Stack stack) {
        this.stack = stack;
    }
}
