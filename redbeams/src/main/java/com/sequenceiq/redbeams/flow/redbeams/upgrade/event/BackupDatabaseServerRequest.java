package com.sequenceiq.redbeams.flow.redbeams.upgrade.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;

public class BackupDatabaseServerRequest extends AbstractUpgradeDatabaseServerRequest {

    @JsonCreator
    public BackupDatabaseServerRequest(
            @JsonProperty("cloudContext") CloudContext cloudContext,
            @JsonProperty("cloudCredential") CloudCredential cloudCredential,
            @JsonProperty("databaseStack") DatabaseStack databaseStack,
            @JsonProperty("targetMajorVersion") TargetMajorVersion targetMajorVersion) {

        super(cloudContext, cloudCredential, databaseStack, targetMajorVersion);
    }

    @Override
    public String toString() {
        return "BackupDatabaseServerRequest{} " + super.toString();
    }

}
