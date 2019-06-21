package com.sequenceiq.redbeams.flow.redbeams.provision.event.register;

import com.google.common.collect.ImmutableList;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;

import java.util.List;

/**
 * A request for registering a database server after allocation.
 */
public class RegisterDatabaseServerRequest extends RedbeamsEvent {

    private final CloudContext cloudContext;

    private final DBStack dbStack;

    private final List<CloudResource> dbResources;

    public RegisterDatabaseServerRequest(CloudContext cloudContext, DBStack dbStack,
        List<CloudResource> dbResources) {
        super(cloudContext != null ? cloudContext.getId() : null);
        this.cloudContext = cloudContext;
        this.dbStack = dbStack;
        this.dbResources = ImmutableList.copyOf(dbResources);
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public DBStack getDBStack() {
        return dbStack;
    }

    public List<CloudResource> getDbResources() {
        return dbResources;
    }

    public String toString() {
        return "RegisterDatabaseServerRequest{"
                + "cloudContext=" + cloudContext
                + ", dbStack=" + dbStack
                + ", dbResources=" + dbResources
                + '}';
    }
}
