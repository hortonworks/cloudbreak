package com.sequenceiq.redbeams.flow.redbeams.common;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

public class RedbeamsContext extends CommonContext {

    private final CloudContext cloudContext;

    private final CloudCredential cloudCredential;

    private final DatabaseStack databaseStack;

    public RedbeamsContext(FlowParameters flowParameters, CloudContext cloudContext, CloudCredential cloudCredential,
        DatabaseStack databaseStack) {
        super(flowParameters);
        this.cloudContext = cloudContext;
        this.cloudCredential = cloudCredential;
        this.databaseStack = databaseStack;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }

    public DatabaseStack getDatabaseStack() {
        return databaseStack;
    }
}
