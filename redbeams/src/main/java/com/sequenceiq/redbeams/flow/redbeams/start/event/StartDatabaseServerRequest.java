package com.sequenceiq.redbeams.flow.redbeams.start.event;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;

/**
 * A request for start  a database server.
 */
public class StartDatabaseServerRequest extends RedbeamsEvent {

    private final CloudContext cloudContext;

    private final CloudCredential cloudCredential;

    private final DatabaseStack dbStack;

    public StartDatabaseServerRequest(CloudContext cloudContext, CloudCredential cloudCredential, DatabaseStack dbStack) {
        super(cloudContext != null ? cloudContext.getId() : null);
        this.cloudContext = cloudContext;
        this.cloudCredential = cloudCredential;
        this.dbStack = dbStack;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }

    public DatabaseStack getDbStack() {
        return dbStack;
    }

    public String toString() {
        return "StartDatabaseServerRequest{"
                + "cloudContext=" + cloudContext
                + ", cloudCredential=" + cloudCredential
                + ", dbStack=" + dbStack
                + '}';
    }
}
