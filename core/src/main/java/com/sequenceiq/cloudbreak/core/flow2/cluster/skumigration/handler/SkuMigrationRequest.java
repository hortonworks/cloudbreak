package com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.handler;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.view.StackView;

public class SkuMigrationRequest extends StackEvent {

    private final StackView stack;

    private final CloudContext cloudContext;

    private final CloudCredential cloudCredential;

    private final CloudConnector cloudConnector;

    private final CloudStack cloudStack;

    @JsonCreator
    protected SkuMigrationRequest(@JsonProperty("stack") StackView stack,
            @JsonProperty("cloudContext") CloudContext cloudContext,
            @JsonProperty("cloudCredential") CloudCredential cloudCredential,
            @JsonProperty("cloudConnector") CloudConnector cloudConnector,
            @JsonProperty("cloudStack") CloudStack cloudStack) {
        super(stack.getId());
        this.stack = stack;
        this.cloudContext = cloudContext;
        this.cloudCredential = cloudCredential;
        this.cloudConnector = cloudConnector;
        this.cloudStack = cloudStack;
    }

    public StackView getStack() {
        return stack;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }

    public CloudConnector getCloudConnector() {
        return cloudConnector;
    }

    public CloudStack getCloudStack() {
        return cloudStack;
    }

    @Override
    public String toString() {
        return "SkuMigrationRequest{" +
                "stack=" + stack +
                ", cloudContext=" + cloudContext +
                ", cloudCredential=" + cloudCredential +
                ", cloudConnector=" + cloudConnector +
                ", cloudStack=" + cloudStack +
                '}';
    }
}
