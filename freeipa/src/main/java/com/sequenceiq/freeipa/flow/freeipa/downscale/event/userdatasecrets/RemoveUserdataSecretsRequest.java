package com.sequenceiq.freeipa.flow.freeipa.downscale.event.userdatasecrets;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class RemoveUserdataSecretsRequest extends StackEvent {

    private final CloudContext cloudContext;

    private final CloudCredential cloudCredential;

    private final List<String> downscaleHosts;

    @JsonCreator
    public RemoveUserdataSecretsRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("cloudContext") CloudContext cloudContext,
            @JsonProperty("cloudCredential") CloudCredential cloudCredential,
            @JsonProperty("downscaleHosts") List<String> downscaleHosts) {
        super(stackId);
        this.cloudContext = cloudContext;
        this.cloudCredential = cloudCredential;
        this.downscaleHosts = downscaleHosts;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }

    public List<String> getDownscaleHosts() {
        return downscaleHosts;
    }

    @Override
    public String toString() {
        return "RemoveUserdataSecretsRequest{" +
                "cloudContext=" + cloudContext +
                ", cloudCredential=" + cloudCredential +
                ", downscaleHosts=" + downscaleHosts +
                "} " + super.toString();
    }
}
