package com.sequenceiq.redbeams.flow.redbeams.sslmigration;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsContext;

public class RedbeamsSslMigrationContext extends RedbeamsContext {

    public RedbeamsSslMigrationContext(FlowParameters flowParameters, CloudContext cloudContext, CloudCredential cloudCredential,
        DatabaseStack databaseStack, DBStack dbStack) {
        super(flowParameters, cloudContext, cloudCredential, databaseStack, dbStack);
    }
}
