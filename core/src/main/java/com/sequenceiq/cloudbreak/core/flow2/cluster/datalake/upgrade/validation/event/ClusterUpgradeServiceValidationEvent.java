package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradeImageInfo;

public class ClusterUpgradeServiceValidationEvent extends ClusterUpgradeValidationEvent {

    // TODO CB-33421: Remove legacy fields once in-flight flow events no longer depend on them in JSON.
    private final boolean lockComponents;

    private final boolean rollingUpgradeEnabled;

    private final String targetRuntime;

    private final UpgradeImageInfo upgradeImageInfo;

    private final boolean replaceVms;

    public ClusterUpgradeServiceValidationEvent(Long resourceId, String imageId, ClusterUpgradeProperties clusterUpgradeProperties) {
        super(ClusterUpgradeValidationHandlerSelectors.VALIDATE_SERVICES_EVENT.name(), resourceId, imageId, clusterUpgradeProperties);
        // TODO CB-33421: Remove legacy field population once in-flight flow events no longer depend on them in JSON.
        this.lockComponents = clusterUpgradeProperties.isLockComponents();
        this.rollingUpgradeEnabled = clusterUpgradeProperties.isRollingUpgradeEnabled();
        this.targetRuntime = clusterUpgradeProperties.getRuntimeVersion();
        this.upgradeImageInfo = null;
        this.replaceVms = clusterUpgradeProperties.isReplaceVms();
    }

    // TODO CB-33421: Remove @JsonCreator once in-flight flow events no longer use the legacy JSON shape.
    @JsonCreator
    public ClusterUpgradeServiceValidationEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("imageId") String imageId,
            @JsonProperty("clusterUpgradeProperties") ClusterUpgradeProperties clusterUpgradeProperties,
            @JsonProperty("lockComponents") boolean lockComponents,
            @JsonProperty("rollingUpgradeEnabled") boolean rollingUpgradeEnabled,
            @JsonProperty("targetRuntime") String targetRuntime,
            @JsonProperty("upgradeImageInfo") UpgradeImageInfo upgradeImageInfo,
            @JsonProperty("replaceVms") boolean replaceVms) {
        super(ClusterUpgradeValidationHandlerSelectors.VALIDATE_SERVICES_EVENT.name(), resourceId, imageId, clusterUpgradeProperties);
        this.lockComponents = lockComponents;
        this.rollingUpgradeEnabled = rollingUpgradeEnabled;
        this.targetRuntime = targetRuntime;
        this.upgradeImageInfo = upgradeImageInfo;
        this.replaceVms = replaceVms;
    }

    public boolean isLockComponents() {
        ClusterUpgradeProperties properties = getClusterUpgradeProperties();
        return properties != null ? properties.isLockComponents() : lockComponents;
    }

    public boolean isRollingUpgradeEnabled() {
        ClusterUpgradeProperties properties = getClusterUpgradeProperties();
        return properties != null ? properties.isRollingUpgradeEnabled() : rollingUpgradeEnabled;
    }

    public String getTargetRuntime() {
        ClusterUpgradeProperties properties = getClusterUpgradeProperties();
        return properties != null ? properties.getRuntimeVersion() : targetRuntime;
    }

    public UpgradeImageInfo getUpgradeImageInfo() {
        // TODO CB-33421: Remove upgradeImageInfo getter once in-flight flow events no longer carry it in JSON.
        return upgradeImageInfo;
    }

    public boolean isReplaceVms() {
        ClusterUpgradeProperties properties = getClusterUpgradeProperties();
        return properties != null ? properties.isReplaceVms() : replaceVms;
    }

    @Override
    public String toString() {
        return "ClusterUpgradeServiceValidationEvent{" +
                "lockComponents=" + isLockComponents() +
                ", rollingUpgradeEnabled=" + isRollingUpgradeEnabled() +
                ", targetRuntime='" + getTargetRuntime() + '\'' +
                ", upgradeImageInfo=" + upgradeImageInfo +
                ", replaceVms=" + isReplaceVms() +
                "} " + super.toString();
    }
}
