package com.sequenceiq.redbeams.flow.redbeams.termination.event.deregister;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;

/**
 * A request for deregistering a database server after termination.
 */
public class DeregisterDatabaseServerRequest extends RedbeamsEvent {

    private final CloudContext cloudContext;

    private final DatabaseStack databaseStack;

    public DeregisterDatabaseServerRequest(CloudContext cloudContext, DatabaseStack databaseStack) {
        super(cloudContext != null ? cloudContext.getId() : null);
        this.cloudContext = cloudContext;
        this.databaseStack = databaseStack;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public DatabaseStack getDatabaseStack() {
        return databaseStack;
    }
}
