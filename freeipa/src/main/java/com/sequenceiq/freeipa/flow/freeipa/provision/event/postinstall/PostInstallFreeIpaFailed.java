package com.sequenceiq.freeipa.flow.freeipa.provision.event.postinstall;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;

public class PostInstallFreeIpaFailed extends StackFailureEvent {
    @JsonCreator
    public PostInstallFreeIpaFailed(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception exception) {
        super(stackId, exception);
    }
}
