package com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.metadata;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class LoadBalancerMetadataCollectionRequest extends StackEvent {

    private final CloudContext cloudContext;

    private final CloudCredential cloudCredential;

    private final CloudStack cloudStack;

    @JsonCreator
    public LoadBalancerMetadataCollectionRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("cloudContext") CloudContext cloudContext,
            @JsonProperty("cloudCredential") CloudCredential cloudCredential,
            @JsonProperty("cloudStack") CloudStack cloudStack) {
        super(stackId);
        this.cloudContext = cloudContext;
        this.cloudCredential = cloudCredential;
        this.cloudStack = cloudStack;
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
        return new StringJoiner(", ", LoadBalancerMetadataCollectionRequest.class.getSimpleName() + "[", "]")
                .add("cloudContext=" + cloudContext)
                .add("cloudCredential=" + cloudCredential)
                .add("cloudStack=" + cloudStack)
                .add(super.toString())
                .toString();
    }
}
