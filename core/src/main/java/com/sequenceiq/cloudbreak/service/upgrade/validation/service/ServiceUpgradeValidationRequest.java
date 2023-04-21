package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import com.sequenceiq.cloudbreak.domain.stack.Stack;

public class ServiceUpgradeValidationRequest {

    private final Stack stack;

    private final boolean lockComponents;

    private final String targetRuntime;

    public ServiceUpgradeValidationRequest(Stack stack, boolean lockComponents, String targetRuntime) {
        this.stack = stack;
        this.lockComponents = lockComponents;
        this.targetRuntime = targetRuntime;
    }

    public Stack getStack() {
        return stack;
    }

    public boolean isLockComponents() {
        return lockComponents;
    }

    public String getTargetRuntime() {
        return targetRuntime;
    }
}
