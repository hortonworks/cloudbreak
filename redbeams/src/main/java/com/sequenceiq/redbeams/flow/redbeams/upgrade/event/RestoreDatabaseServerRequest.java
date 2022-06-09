package com.sequenceiq.redbeams.flow.redbeams.upgrade.event;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;

public class RestoreDatabaseServerRequest extends AbstractUpgradeDatabaseServerRequest {

    public RestoreDatabaseServerRequest(CloudContext cloudContext, CloudCredential cloudCredential, DatabaseStack databaseStack,
            TargetMajorVersion targetMajorVersion) {
        super(cloudContext, cloudCredential, databaseStack, targetMajorVersion);
    }

    @Override
    public String toString() {
        return "RestoreDatabaseServerRequest{} " + super.toString();
    }

}