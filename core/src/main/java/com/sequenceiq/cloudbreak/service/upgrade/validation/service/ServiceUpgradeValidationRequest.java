package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradeImageInfo;

public record ServiceUpgradeValidationRequest(
        StackDto stack,
        // TODO CB-33421: Remove lockComponents and rollingUpgradeEnabled once callers use clusterUpgradeProperties.
        boolean lockComponents,
        boolean rollingUpgradeEnabled,
        // TODO CB-33421: Remove upgradeImageInfo once callers and in-flight flow events no longer use it.
        UpgradeImageInfo upgradeImageInfo,
        ClusterUpgradeProperties clusterUpgradeProperties,
        boolean replaceVms) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private StackDto stack;

        private boolean lockComponents;

        private boolean rollingUpgradeEnabled;

        private UpgradeImageInfo upgradeImageInfo;

        private ClusterUpgradeProperties clusterUpgradeProperties;

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

        public Builder withClusterUpgradeProperties(ClusterUpgradeProperties clusterUpgradeProperties) {
            this.clusterUpgradeProperties = clusterUpgradeProperties;
            return this;
        }

        public Builder withReplaceVms(boolean replaceVms) {
            this.replaceVms = replaceVms;
            return this;
        }

        public ServiceUpgradeValidationRequest build() {
            return new ServiceUpgradeValidationRequest(stack, lockComponents, rollingUpgradeEnabled, upgradeImageInfo, clusterUpgradeProperties, replaceVms);
        }
    }
}
