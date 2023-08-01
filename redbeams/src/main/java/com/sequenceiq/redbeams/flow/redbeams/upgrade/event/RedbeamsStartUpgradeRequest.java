package com.sequenceiq.redbeams.flow.redbeams.upgrade.event;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.redbeams.dto.UpgradeDatabaseMigrationParams;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsUpgradeEvent;

public class RedbeamsStartUpgradeRequest extends RedbeamsEvent {

    private final TargetMajorVersion targetMajorVersion;

    private final UpgradeDatabaseMigrationParams migrationParams;

    @JsonCreator
    public RedbeamsStartUpgradeRequest(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("targetMajorVersion") TargetMajorVersion targetMajorVersion,
            @JsonProperty("migrationParams") UpgradeDatabaseMigrationParams migrationParams) {

        super(RedbeamsUpgradeEvent.REDBEAMS_START_UPGRADE_EVENT.selector(), resourceId);
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
        return "RedbeamsStartUpgradeRequest{" +
                "targetMajorVersion=" + targetMajorVersion +
                ", migrationParams=" + migrationParams +
                "} " + super.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RedbeamsStartUpgradeRequest that = (RedbeamsStartUpgradeRequest) o;
        if (targetMajorVersion != that.targetMajorVersion) {
            return false;
        }
        return Objects.equals(migrationParams, that.migrationParams);
    }

    @Override
    public int hashCode() {
        int result = targetMajorVersion != null ? targetMajorVersion.hashCode() : 0;
        result = 31 * result + (migrationParams != null ? migrationParams.hashCode() : 0);
        return result;
    }
}
