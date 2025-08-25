package com.sequenceiq.redbeams.flow.redbeams.sslmigration.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;

public class RedbeamsSslMigrationHandlerRequest extends RedbeamsEvent {

    private final CloudContext cloudContext;

    private final CloudCredential cloudCredential;

    private final DatabaseStack databaseStack;

    @JsonCreator
    public RedbeamsSslMigrationHandlerRequest(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("cloudContext") CloudContext cloudContext,
            @JsonProperty("cloudCredential") CloudCredential cloudCredential,
            @JsonProperty("databaseStack") DatabaseStack databaseStack) {
        super(EventSelectorUtil.selector(RedbeamsSslMigrationHandlerRequest.class), resourceId, new Promise<>(), true);
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
        return "SslCertRotateDatabaseServerRequest{"
                + "cloudContext=" + cloudContext
                + ", cloudCredential=" + cloudCredential
                + ", databaseStack=" + databaseStack
                + '}';
    }
}
