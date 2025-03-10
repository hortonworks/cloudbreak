package com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.handler.check;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.handler.SkuMigrationRequest;
import com.sequenceiq.cloudbreak.view.StackView;

public class CheckSkuRequest extends SkuMigrationRequest {

    private final boolean force;

    @JsonCreator
    public CheckSkuRequest(@JsonProperty("stack") StackView stack,
            @JsonProperty("cloudContext") CloudContext cloudContext,
            @JsonProperty("cloudCredential") CloudCredential cloudCredential,
            @JsonProperty("cloudConnector") CloudConnector cloudConnector,
            @JsonProperty("cloudStack") CloudStack cloudStack,
            @JsonProperty("force") boolean force) {
        super(stack, cloudContext, cloudCredential, cloudConnector, cloudStack);
        this.force = force;
    }

    public boolean isForce() {
        return force;
    }

    @Override
    public String toString() {
        return "CheckSkuRequest{" +
                "force=" + force +
                "} " + super.toString();
    }
}
