package com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class DownscaleRemoveUserdataSecretsRequest extends StackEvent {

    private final CloudContext cloudContext;

    private final CloudCredential cloudCredential;

    private final List<Long> instancePrivateIds;

    @JsonCreator
    public DownscaleRemoveUserdataSecretsRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("cloudContext") CloudContext cloudContext,
            @JsonProperty("cloudCredential") CloudCredential cloudCredential,
            @JsonProperty("instancePrivateIds") List<Long> instancePrivateIds) {
        super(stackId);
        this.cloudContext = cloudContext;
        this.cloudCredential = cloudCredential;
        this.instancePrivateIds = instancePrivateIds;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }

    public List<Long> getInstancePrivateIds() {
        return instancePrivateIds;
    }

    @Override
    public String toString() {
        return "DownscaleRemoveUserdataSecretsRequest{" +
                "cloudContext=" + cloudContext +
                ", cloudCredential=" + cloudCredential +
                ", instancePrivateIds=" + instancePrivateIds +
                "} " + super.toString();
    }
}
