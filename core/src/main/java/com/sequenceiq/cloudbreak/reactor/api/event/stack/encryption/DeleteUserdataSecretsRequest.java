package com.sequenceiq.cloudbreak.reactor.api.event.stack.encryption;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.TerminationEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.TerminationType;
import com.sequenceiq.flow.event.EventSelectorUtil;

public class DeleteUserdataSecretsRequest extends TerminationEvent {

    private final CloudContext cloudContext;

    private final CloudCredential cloudCredential;

    @JsonCreator
    public DeleteUserdataSecretsRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("terminationType") TerminationType terminationType,
            @JsonProperty("cloudContext") CloudContext cloudContext,
            @JsonProperty("cloudCredential") CloudCredential cloudCredential) {
        super(EventSelectorUtil.selector(DeleteUserdataSecretsRequest.class), stackId, terminationType);
        this.cloudContext = cloudContext;
        this.cloudCredential = cloudCredential;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }

    @Override
    public String toString() {
        return "DeleteUserdataSecretsRequest{" +
                "cloudContext=" + cloudContext +
                ", cloudCredential=" + cloudCredential +
                "} " + super.toString();
    }
}
