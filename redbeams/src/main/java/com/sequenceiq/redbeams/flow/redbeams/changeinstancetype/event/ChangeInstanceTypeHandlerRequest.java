package com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.database.DatabaseServerInstanceTypeState;

public class ChangeInstanceTypeHandlerRequest extends ChangeInstanceTypeEvent {

    private final CloudContext cloudContext;

    private final CloudCredential cloudCredential;

    private final DatabaseStack databaseStack;

    private final String environmentCrn;

    @JsonCreator
    public ChangeInstanceTypeHandlerRequest(
            @JsonProperty("selector") String selector,
            @JsonProperty("cloudContext") CloudContext cloudContext,
            @JsonProperty("cloudCredential") CloudCredential cloudCredential,
            @JsonProperty("databaseStack") DatabaseStack databaseStack,
            @JsonProperty("environmentCrn") String environmentCrn,
            @JsonProperty("targetInstanceType") String targetInstanceType,
            @JsonProperty("liveState") DatabaseServerInstanceTypeState liveState) {
        super(selector, cloudContext != null ? cloudContext.getId() : null, targetInstanceType, liveState);
        this.cloudContext = cloudContext;
        this.cloudCredential = cloudCredential;
        this.databaseStack = databaseStack;
        this.environmentCrn = environmentCrn;
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

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    @Override
    public String toString() {
        return "ChangeInstanceTypeHandlerRequest{" +
                "cloudContext=" + cloudContext +
                ", databaseStack=" + databaseStack +
                ", environmentCrn='" + environmentCrn + '\'' +
                "} " + super.toString();
    }
}
