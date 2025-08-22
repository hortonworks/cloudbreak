package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradeImageInfo;

public record ServiceUpgradeValidationRequest(
        StackDto stack,
        boolean lockComponents,
        boolean rollingUpgradeEnabled,
        UpgradeImageInfo upgradeImageInfo,
        boolean replaceVms) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private StackDto stack;

        private boolean lockComponents;

        private boolean rollingUpgradeEnabled;

        private UpgradeImageInfo upgradeImageInfo;

        private boolean replaceVms;

        private Builder() {
        }

        public Builder withStack(StackDto stack) {
            this.stack = stack;
            return this;
        }

        public Builder withLockComponents(boolean lockComponents) {
            this.lockComponents = lockComponents;
            return this;
        }

        public Builder withRollingUpgradeEnabled(boolean rollingUpgradeEnabled) {
            this.rollingUpgradeEnabled = rollingUpgradeEnabled;
            return this;
        }

        public Builder withUpgradeImageInfo(UpgradeImageInfo upgradeImageInfo) {
            this.upgradeImageInfo = upgradeImageInfo;
            return this;
        }

        public Builder withReplaceVms(boolean replaceVms) {
            this.replaceVms = replaceVms;
            return this;
        }

        public ServiceUpgradeValidationRequest build() {
            return new ServiceUpgradeValidationRequest(stack, lockComponents, rollingUpgradeEnabled, upgradeImageInfo, replaceVms);
        }
    }
}
