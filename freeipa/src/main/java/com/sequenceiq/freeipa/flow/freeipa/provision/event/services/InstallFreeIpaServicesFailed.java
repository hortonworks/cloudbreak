package com.sequenceiq.freeipa.flow.freeipa.provision.event.services;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;

public class InstallFreeIpaServicesFailed extends StackFailureEvent {
    @JsonCreator
    public InstallFreeIpaServicesFailed(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception exception) {
        super(stackId, exception);
    }
}
