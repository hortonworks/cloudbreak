package com.sequenceiq.redbeams.flow.redbeams.provision.event.allocate;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.event.CancellationToken;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;

/**
 * A request for allocating a database server.
 */
public class AllocateDatabaseServerRequest extends RedbeamsEvent {

    private final CloudContext cloudContext;

    private final CloudCredential cloudCredential;

    private final DatabaseStack databaseStack;

    private final CancellationToken cancellationToken;

    public AllocateDatabaseServerRequest(CloudContext cloudContext, CloudCredential cloudCredential, DatabaseStack databaseStack,
            CancellationToken cancellationToken) {
        super(cloudContext != null ? cloudContext.getId() : null);
        this.cloudContext = cloudContext;
        this.cloudCredential = cloudCredential;
        this.databaseStack = databaseStack;
        this.cancellationToken = cancellationToken;
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

    public CancellationToken getCancellationToken() {
        return cancellationToken;
    }

    public String toString() {
        return "AllocateDatabaseServerRequest{"
            + "cloudContext=" + cloudContext
            + ", cloudCredential=" + cloudCredential
            + ", databaseStack=" + databaseStack
            + '}';
    }
}
