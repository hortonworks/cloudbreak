package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradeImageInfo;

public class ClusterUpgradeServiceValidationEvent extends StackEvent {

    private final boolean lockComponents;

    private final boolean rollingUpgradeEnabled;

    private final String targetRuntime;

    private final UpgradeImageInfo upgradeImageInfo;

    @JsonCreator
    public ClusterUpgradeServiceValidationEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("lockComponents") boolean lockComponents,
            @JsonProperty("rollingUpgradeEnabled") boolean rollingUpgradeEnabled,
            @JsonProperty("targetRuntime") String targetRuntime,
            @JsonProperty("upgradeImageInfo") UpgradeImageInfo upgradeImageInfo) {
        super(ClusterUpgradeValidationHandlerSelectors.VALIDATE_SERVICES_EVENT.name(), resourceId);
        this.lockComponents = lockComponents;
        this.rollingUpgradeEnabled = rollingUpgradeEnabled;
        this.targetRuntime = targetRuntime;
        this.upgradeImageInfo = upgradeImageInfo;
    }

    public boolean isLockComponents() {
        return lockComponents;
    }

    public boolean isRollingUpgradeEnabled() {
        return rollingUpgradeEnabled;
    }

    public String getTargetRuntime() {
        return targetRuntime;
    }

    public UpgradeImageInfo getUpgradeImageInfo() {
        return upgradeImageInfo;
    }

    @Override
    public String toString() {
        return "ClusterUpgradeServiceValidationEvent{" +
                "lockComponents=" + lockComponents +
                "rollingUpgradeEnabled=" + rollingUpgradeEnabled +
                "targetRuntime=" + targetRuntime +
                "upgradeImageInfo=" + upgradeImageInfo +
                "} " + super.toString();
    }
}
