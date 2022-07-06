package com.sequenceiq.freeipa.flow.freeipa.provision.event.postinstall;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class PostInstallFreeIpaSuccess extends StackEvent {
    @JsonCreator
    public PostInstallFreeIpaSuccess(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
