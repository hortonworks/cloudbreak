package com.sequenceiq.redbeams.flow.redbeams.common;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.redbeams.domain.stack.DBStack;

public class RedbeamsContext extends CommonContext {

    private final CloudContext cloudContext;

    private final CloudCredential cloudCredential;

    private final DatabaseStack databaseStack;

    private final DBStack dbStack;

    public RedbeamsContext(FlowParameters flowParameters, CloudContext cloudContext, CloudCredential cloudCredential,
        DatabaseStack databaseStack, DBStack dbStack) {
        super(flowParameters);
        this.cloudContext = cloudContext;
        this.cloudCredential = cloudCredential;
        this.databaseStack = databaseStack;
        this.dbStack = dbStack;
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

    public DBStack getDBStack() {
        return dbStack;
    }

    public boolean doesDBStackExist() {
        return dbStack != null;
    }
}
