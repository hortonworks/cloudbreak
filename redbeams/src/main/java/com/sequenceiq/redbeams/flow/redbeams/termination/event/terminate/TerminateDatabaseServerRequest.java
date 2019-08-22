package com.sequenceiq.redbeams.flow.redbeams.termination.event.terminate;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;

/**
 * A request for terminating a database server.
 */
public class TerminateDatabaseServerRequest extends RedbeamsEvent {

    private final CloudContext cloudContext;

    private final CloudCredential cloudCredential;

    private final DatabaseStack databaseStack;

    public TerminateDatabaseServerRequest(CloudContext cloudContext, CloudCredential cloudCredential, DatabaseStack databaseStack, boolean forced) {
        super(cloudContext != null ? cloudContext.getId() : null, forced);
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

    public String toString() {
        return "TerminateDatabaseServerRequest{"
            + "cloudContext=" + cloudContext
            + ", cloudCredential=" + cloudCredential
            + ", databaseStack=" + databaseStack
            + '}';
    }
}
