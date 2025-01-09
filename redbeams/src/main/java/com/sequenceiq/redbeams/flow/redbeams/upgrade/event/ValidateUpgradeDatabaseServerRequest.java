package com.sequenceiq.redbeams.flow.redbeams.upgrade.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.redbeams.dto.UpgradeDatabaseMigrationParams;

public class ValidateUpgradeDatabaseServerRequest extends AbstractUpgradeDatabaseServerRequest {

    private final UpgradeDatabaseMigrationParams migrationParams;

    @JsonCreator
    public ValidateUpgradeDatabaseServerRequest(
            @JsonProperty("cloudContext") CloudContext cloudContext,
            @JsonProperty("cloudCredential") CloudCredential cloudCredential,
            @JsonProperty("databaseStack") DatabaseStack databaseStack,
            @JsonProperty("targetMajorVersion") TargetMajorVersion targetMajorVersion,
            @JsonProperty("migrationParams") UpgradeDatabaseMigrationParams migrationParams) {
        super(cloudContext, cloudCredential, databaseStack, targetMajorVersion);
        this.migrationParams = migrationParams;
    }

    public UpgradeDatabaseMigrationParams getMigrationParams() {
        return migrationParams;
    }

    @Override
    public String toString() {
        return "UpgradeDatabaseServerRequest{" +
                "migrationParams=" + migrationParams +
                "} " + super.toString();
    }
}