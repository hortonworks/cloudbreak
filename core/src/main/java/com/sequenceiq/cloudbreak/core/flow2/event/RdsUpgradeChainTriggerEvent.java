package com.sequenceiq.cloudbreak.core.flow2.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class RdsUpgradeChainTriggerEvent extends StackEvent {

    private final TargetMajorVersion version;

    private final String backupLocation;

    private final String backupInstanceProfile;

    @JsonCreator
    public RdsUpgradeChainTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("version") TargetMajorVersion version,
            @JsonProperty("backupLocation") String backupLocation,
            @JsonProperty("backupInstanceProfile") String backupInstanceProfile) {
        super(selector, stackId);
        this.backupLocation = backupLocation;
        this.backupInstanceProfile = backupInstanceProfile;
        this.version = version;
    }

    public TargetMajorVersion getVersion() {
        return version;
    }

    public String getBackupLocation() {
        return backupLocation;
    }

    public String getBackupInstanceProfile() {
        return backupInstanceProfile;
    }

    @Override
    public String toString() {
        return "RdsUpgradeChainTriggerEvent{" +
                "version=" + version +
                ", backupLocation='" + backupLocation + '\'' +
                ", backupInstanceProfile='" + backupInstanceProfile + '\'' +
                "} " + super.toString();
    }
}
