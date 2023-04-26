package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradeImageInfo;

public class ServiceUpgradeValidationRequest {

    private final StackDto stack;

    private final boolean lockComponents;

    private final String targetRuntime;

    private final UpgradeImageInfo upgradeImageInfo;

    public ServiceUpgradeValidationRequest(StackDto stack, boolean lockComponents, String targetRuntime, UpgradeImageInfo upgradeImageInfo) {
        this.stack = stack;
        this.lockComponents = lockComponents;
        this.targetRuntime = targetRuntime;
        this.upgradeImageInfo = upgradeImageInfo;
    }

    public StackDto getStack() {
        return stack;
    }

    public boolean isLockComponents() {
        return lockComponents;
    }

    public String getTargetRuntime() {
        return targetRuntime;
    }

    public UpgradeImageInfo getUpgradeImageInfo() {
        return upgradeImageInfo;
    }
}
