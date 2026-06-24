package com.sequenceiq.redbeams.flow.redbeams.upgrade.chain;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.redbeams.dto.UpgradeDatabaseMigrationParams;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;

public class RedbeamsUpgradeFlowChainTriggerEvent extends RedbeamsEvent {

    private final TargetMajorVersion targetMajorVersion;

    private final UpgradeDatabaseMigrationParams migrationParams;

    public RedbeamsUpgradeFlowChainTriggerEvent(Long resourceId) {
        this(null, resourceId, null, null);
    }

    @JsonCreator
    public RedbeamsUpgradeFlowChainTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("targetMajorVersion") TargetMajorVersion targetMajorVersion,
            @JsonProperty("migrationParams") UpgradeDatabaseMigrationParams migrationParams) {
        super(selector, resourceId);
        this.targetMajorVersion = targetMajorVersion;
        this.migrationParams = migrationParams;
    }

    public TargetMajorVersion getTargetMajorVersion() {
        return targetMajorVersion;
    }

    public UpgradeDatabaseMigrationParams getMigrationParams() {
        return migrationParams;
    }

    @Override
    public String toString() {
        return "RedbeamsUpgradeFlowChainTriggerEvent{" +
                "targetMajorVersion=" + targetMajorVersion +
                ", migrationParams=" + migrationParams +
                "} " + super.toString();
    }

    @Override
    public boolean equalsEvent(RedbeamsEvent other) {
        return isClassAndEqualsEvent(RedbeamsUpgradeFlowChainTriggerEvent.class, other,
                event -> Objects.equals(migrationParams, event.migrationParams)
                        && targetMajorVersion == event.targetMajorVersion);
    }
}
