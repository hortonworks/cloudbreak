package com.sequenceiq.redbeams.flow.redbeams.provision.event.register;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;

/**
 * A request for registering a database server after allocation.
 */
public class UpdateDatabaseServerRegistrationRequest extends RedbeamsEvent {

    private final CloudContext cloudContext;

    private final CloudCredential cloudCredential;

    private final DBStack dbStack;

    private final DatabaseStack databaseStack;

    private final List<CloudResource> dbResources;

    public UpdateDatabaseServerRegistrationRequest(CloudContext cloudContext, CloudCredential cloudCredential, DBStack dbStack,
            DatabaseStack databaseStack, List<CloudResource> dbResources) {
        super(cloudContext != null ? cloudContext.getId() : null);
        this.cloudContext = cloudContext;
        this.cloudCredential = cloudCredential;
        this.dbStack = dbStack;
        this.databaseStack = databaseStack;
        this.dbResources = ImmutableList.copyOf(dbResources);
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }

    public DBStack getDBStack() {
        return dbStack;
    }

    public DatabaseStack getDatabaseStack() {
        return databaseStack;
    }

    public List<CloudResource> getDbResources() {
        return dbResources;
    }

    @Override
    public String toString() {
        return "UpdateDatabaseServerRegistrationRequest{" +
                "cloudContext=" + cloudContext +
                ", cloudCredential=" + cloudCredential +
                ", dbStack=" + dbStack +
                ", databaseStack=" + databaseStack +
                ", dbResources=" + dbResources +
                '}';
    }

}
