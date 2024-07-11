package com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class UpscaleUpdateUserdataSecretsRequest extends StackEvent {

    private final CloudContext cloudContext;

    private final CloudCredential cloudCredential;

    private final List<Long> newInstanceIds;

    @JsonCreator
    public UpscaleUpdateUserdataSecretsRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("cloudContext") CloudContext cloudContext,
            @JsonProperty("cloudCredential") CloudCredential cloudCredential,
            @JsonProperty("newInstanceIds") List<Long> newInstanceIds) {
        super(stackId);
        this.cloudContext = cloudContext;
        this.cloudCredential = cloudCredential;
        this.newInstanceIds = newInstanceIds;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }

    public List<Long> getNewInstanceIds() {
        return newInstanceIds;
    }

    @Override
    public String toString() {
        return "UpscaleUpdateUserdataSecretsRequest{" +
                "cloudContext=" + cloudContext +
                ", cloudCredential=" + cloudCredential +
                ", newInstanceIds=" + newInstanceIds +
                "} " + super.toString();
    }
}
