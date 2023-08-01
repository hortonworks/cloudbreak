package com.sequenceiq.redbeams.flow.redbeams.upgrade.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.redbeams.dto.UpgradeDatabaseMigrationParams;

public class UpgradeDatabaseServerRequest extends AbstractUpgradeDatabaseServerRequest {

    private final UpgradeDatabaseMigrationParams upgradeDatabaseMigrationParams;

    @JsonCreator
    public UpgradeDatabaseServerRequest(
            @JsonProperty("cloudContext") CloudContext cloudContext,
            @JsonProperty("cloudCredential") CloudCredential cloudCredential,
            @JsonProperty("databaseStack") DatabaseStack databaseStack,
            @JsonProperty("targetMajorVersion") TargetMajorVersion targetMajorVersion,
            @JsonProperty("upgradeDatabaseMigrationParams") UpgradeDatabaseMigrationParams upgradeDatabaseMigrationParams) {
        super(cloudContext, cloudCredential, databaseStack, targetMajorVersion);
        this.upgradeDatabaseMigrationParams = upgradeDatabaseMigrationParams;
    }

    public UpgradeDatabaseMigrationParams getUpgradeDatabaseMigrationParams() {
        return upgradeDatabaseMigrationParams;
    }

    @Override
    public String toString() {
        return "UpgradeDatabaseServerRequest{" +
                "upgradeDatabaseMigrationParams=" + upgradeDatabaseMigrationParams +
                "} " + super.toString();
    }
}
