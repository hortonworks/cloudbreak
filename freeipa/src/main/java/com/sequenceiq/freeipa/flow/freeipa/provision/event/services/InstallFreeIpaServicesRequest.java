package com.sequenceiq.freeipa.flow.freeipa.provision.event.services;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class InstallFreeIpaServicesRequest extends StackEvent {
    @JsonCreator
    public InstallFreeIpaServicesRequest(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
