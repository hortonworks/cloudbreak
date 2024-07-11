package com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class UpscaleUpdateUserdataSecretsSuccess extends StackEvent {

    @JsonCreator
    public UpscaleUpdateUserdataSecretsSuccess(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
