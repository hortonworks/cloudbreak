package com.sequenceiq.redbeams.flow.redbeams.termination.event.deregister;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;

/**
 * A request for deregistering a database server after termination.
 */
public class DeregisterDatabaseServerRequest extends RedbeamsEvent {

    private final CloudContext cloudContext;

    private final DatabaseStack databaseStack;

    private final DBStack dbStack;

    public DeregisterDatabaseServerRequest(CloudContext cloudContext, DatabaseStack databaseStack, DBStack dbStack) {
        super(cloudContext != null ? cloudContext.getId() : null);
        this.cloudContext = cloudContext;
        this.databaseStack = databaseStack;
        this.dbStack = dbStack;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public DatabaseStack getDatabaseStack() {
        return databaseStack;
    }

    public DBStack getDbStack() {
        return dbStack;
    }
}
