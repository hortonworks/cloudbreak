package com.sequenceiq.freeipa.flow.stack.termination.event.secret;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.flow.stack.termination.event.TerminationEvent;

public class DeleteUserdataSecretsRequest extends TerminationEvent {

    private final CloudContext cloudContext;

    private final CloudCredential cloudCredential;

    @JsonCreator
    public DeleteUserdataSecretsRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("forced") Boolean forced,
            @JsonProperty("cloudContext") CloudContext cloudContext,
            @JsonProperty("cloudCredential") CloudCredential cloudCredential) {
        super(EventSelectorUtil.selector(DeleteUserdataSecretsRequest.class), stackId, forced);
        this.cloudContext = cloudContext;
        this.cloudCredential = cloudCredential;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }
}
