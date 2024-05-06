package com.sequenceiq.freeipa.flow.freeipa.upscale.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class UpscaleUpdateUserdataSecretsSuccess extends StackEvent {

    @JsonCreator
    public UpscaleUpdateUserdataSecretsSuccess(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
