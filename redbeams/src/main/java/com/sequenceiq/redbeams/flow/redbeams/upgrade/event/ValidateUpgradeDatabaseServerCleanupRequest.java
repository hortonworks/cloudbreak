package com.sequenceiq.redbeams.flow.redbeams.upgrade.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;

public class ValidateUpgradeDatabaseServerCleanupRequest extends AbstractUpgradeDatabaseServerRequest {

    @JsonCreator
    public ValidateUpgradeDatabaseServerCleanupRequest(
            @JsonProperty("cloudContext") CloudContext cloudContext,
            @JsonProperty("cloudCredential") CloudCredential cloudCredential,
            @JsonProperty("databaseStack") DatabaseStack databaseStack) {
        super(cloudContext, cloudCredential, databaseStack, null);
    }

    @Override
    public String toString() {
        return "ValidateUpgradeDatabaseServerCleanupRequest{} " + super.toString();
    }
}