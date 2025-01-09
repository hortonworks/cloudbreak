package com.sequenceiq.redbeams.flow.redbeams.upgrade.event;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.redbeams.dto.UpgradeDatabaseMigrationParams;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsValidateUpgradeEvent;

public class RedbeamsStartValidateUpgradeRequest extends RedbeamsEvent {

    private final TargetMajorVersion targetMajorVersion;

    private final UpgradeDatabaseMigrationParams migrationParams;

    @JsonCreator
    public RedbeamsStartValidateUpgradeRequest(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("targetMajorVersion") TargetMajorVersion targetMajorVersion,
            @JsonProperty("migrationParams") UpgradeDatabaseMigrationParams migrationParams) {
        super(RedbeamsValidateUpgradeEvent.REDBEAMS_START_VALIDATE_UPGRADE_EVENT.selector(), resourceId);
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
        return "RedbeamsStartValidateUpgradeRequest{" +
                "targetMajorVersion=" + targetMajorVersion +
                ", migrationParams=" + migrationParams +
                "} " + super.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RedbeamsStartValidateUpgradeRequest that = (RedbeamsStartValidateUpgradeRequest) o;
        return targetMajorVersion == that.targetMajorVersion && Objects.equals(migrationParams, that.migrationParams);
    }

    @Override
    public int hashCode() {
        return Objects.hash(targetMajorVersion, migrationParams);
    }

    @Override
    public boolean equalsEvent(RedbeamsEvent other) {
        return isClassAndEqualsEvent(RedbeamsStartValidateUpgradeRequest.class, other,
                event -> Objects.equals(migrationParams, event.migrationParams)
                        && targetMajorVersion == event.targetMajorVersion);
    }
}