package com.sequenceiq.freeipa.flow.freeipa.migration.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class MultiAzMigrationLbUpdateHandlerRequest extends StackEvent {

    private final String operationId;

    private final CloudContext cloudContext;

    private final CloudCredential cloudCredential;

    private final CloudStack cloudStack;

    @JsonCreator
    public MultiAzMigrationLbUpdateHandlerRequest(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("operationId") String operationId,
            @JsonProperty("cloudContext") CloudContext cloudContext,
            @JsonProperty("cloudCredential") CloudCredential cloudCredential,
            @JsonProperty("cloudStack") CloudStack cloudStack) {
        super(resourceId);
        this.operationId = operationId;
        this.cloudContext = cloudContext;
        this.cloudCredential = cloudCredential;
        this.cloudStack = cloudStack;
    }

    public String getOperationId() {
        return operationId;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }

    public CloudStack getCloudStack() {
        return cloudStack;
    }

    @Override
    public String toString() {
        return "MultiAzMigrationLbUpdateHandlerRequest{" +
                "operationId='" + operationId + '\'' +
                ", cloudContext=" + cloudContext +
                ", cloudCredential=" + cloudCredential +
                ", cloudStack=" + cloudStack +
                "} " + super.toString();
    }
}
