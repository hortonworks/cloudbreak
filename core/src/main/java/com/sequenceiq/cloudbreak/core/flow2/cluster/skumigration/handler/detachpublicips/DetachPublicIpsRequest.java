package com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.handler.detachpublicips;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.handler.SkuMigrationRequest;
import com.sequenceiq.cloudbreak.view.StackView;

public class DetachPublicIpsRequest extends SkuMigrationRequest {

    @JsonCreator
    public DetachPublicIpsRequest(@JsonProperty("stack") StackView stack,
            @JsonProperty("cloudContext") CloudContext cloudContext,
            @JsonProperty("cloudCredential") CloudCredential cloudCredential,
            @JsonProperty("cloudConnector") CloudConnector cloudConnector,
            @JsonProperty("cloudStack") CloudStack cloudStack) {
        super(stack, cloudContext, cloudCredential, cloudConnector, cloudStack);
    }

    @Override
    public String toString() {
        return "DetachPublicIpsRequest{} " + super.toString();
    }
}
