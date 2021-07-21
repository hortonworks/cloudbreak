package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import com.sequenceiq.cloudbreak.domain.stack.Stack;

public class ServiceUpgradeValidationRequest {

    private final Stack stack;

    private final boolean lockComponents;

    public ServiceUpgradeValidationRequest(Stack stack, boolean lockComponents) {
        this.stack = stack;
        this.lockComponents = lockComponents;
    }

    public Stack getStack() {
        return stack;
    }

    public boolean isLockComponents() {
        return lockComponents;
    }
}
