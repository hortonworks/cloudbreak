package com.sequenceiq.freeipa.flow.freeipa.downscale.event.userdatasecrets;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class RemoveUserdataSecretsSuccess extends StackEvent {

    @JsonCreator
    public RemoveUserdataSecretsSuccess(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
