package com.sequenceiq.cloudbreak.service;

import com.sequenceiq.cloudbreak.cluster.service.StackAware;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;

public abstract class StackContext implements StackAware {

    private final StackDtoDelegate stack;

    protected StackContext(StackDtoDelegate stack) {
        this.stack = stack;
    }

    public StackDtoDelegate getStack() {
        return stack;
    }
}
