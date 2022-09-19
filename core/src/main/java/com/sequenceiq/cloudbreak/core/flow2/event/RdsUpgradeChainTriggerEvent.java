package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class RdsUpgradeChainTriggerEvent extends StackEvent {

    private final TargetMajorVersion version;

    private final String backupLocation;

    @JsonCreator
    public RdsUpgradeChainTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("version") TargetMajorVersion version,
            @JsonProperty("backupLocation") String backupLocation) {
        super(selector, stackId);
        this.backupLocation = backupLocation;
        this.version = version;
    }

    public TargetMajorVersion getVersion() {
        return version;
    }

    public String getBackupLocation() {
        return backupLocation;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", RdsUpgradeChainTriggerEvent.class.getSimpleName() + "[", "]")
                .add("version=" + version)
                .add("backupLocation='" + backupLocation + "'")
                .toString();
    }
}
