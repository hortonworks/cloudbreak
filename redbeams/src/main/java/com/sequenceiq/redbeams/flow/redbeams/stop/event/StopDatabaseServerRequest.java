package com.sequenceiq.redbeams.flow.redbeams.stop.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;

/**
 * A request for stop  a database server.
 */
public class StopDatabaseServerRequest extends RedbeamsEvent {

    private final CloudContext cloudContext;

    private final CloudCredential cloudCredential;

    private final DatabaseStack dbStack;

    @JsonCreator
    public StopDatabaseServerRequest(
            @JsonProperty("cloudContext") CloudContext cloudContext,
            @JsonProperty("cloudCredential") CloudCredential cloudCredential,
            @JsonProperty("dbStack") DatabaseStack dbStack) {

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
        return "StopDatabaseServerRequest{"
                + "cloudContext=" + cloudContext
                + ", cloudCredential=" + cloudCredential
                + ", dbStack=" + dbStack
                + '}';
    }
}
