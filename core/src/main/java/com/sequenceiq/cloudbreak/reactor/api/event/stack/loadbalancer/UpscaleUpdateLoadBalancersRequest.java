package com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class UpscaleUpdateLoadBalancersRequest extends StackEvent {

    private final CloudStack cloudStack;

    private final CloudContext cloudContext;

    private final CloudCredential cloudCredential;

    @JsonCreator
    public UpscaleUpdateLoadBalancersRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("cloudStack") CloudStack cloudStack,
            @JsonProperty("cloudContext") CloudContext cloudContext,
            @JsonProperty("cloudCredential") CloudCredential cloudCredential) {
        super(stackId);
        this.cloudStack = cloudStack;
        this.cloudContext = cloudContext;
        this.cloudCredential = cloudCredential;
    }

    public CloudStack getCloudStack() {
        return cloudStack;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }

    @Override
    public String toString() {
        return "UpscaleUpdateLoadBalancersRequest{" +
                "cloudStack=" + cloudStack +
                ", cloudContext=" + cloudContext +
                ", cloudCredential=" + cloudCredential +
                "} " + super.toString();
    }
}
