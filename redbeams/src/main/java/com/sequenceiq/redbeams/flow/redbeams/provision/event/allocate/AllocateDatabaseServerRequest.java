package com.sequenceiq.redbeams.flow.redbeams.provision.event.allocate;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;

/**
 * A request for allocating a database server.
 */
public class AllocateDatabaseServerRequest extends RedbeamsEvent {

    private final CloudContext cloudContext;

    private final CloudCredential cloudCredential;

    private final DBStack dbStack;

    private final DatabaseStack databaseStack;

    public AllocateDatabaseServerRequest(CloudContext cloudContext, CloudCredential cloudCredential, DBStack dbStack, DatabaseStack databaseStack) {
        super(cloudContext != null ? cloudContext.getId() : null);
        this.cloudContext = cloudContext;
        this.cloudCredential = cloudCredential;
        this.dbStack = dbStack;
        this.databaseStack = databaseStack;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }

    public DBStack getDbStack() {
        return dbStack;
    }

    public DatabaseStack getDatabaseStack() {
        return databaseStack;
    }

    @Override
    public String toString() {
        return "AllocateDatabaseServerRequest{" +
                "cloudContext=" + cloudContext +
                ", cloudCredential=" + cloudCredential +
                ", dbStack=" + dbStack +
                ", databaseStack=" + databaseStack +
                '}';
    }

}
