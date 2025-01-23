package com.sequenceiq.cloudbreak.reactor.api.event.stack;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class UpscaleStackSaltValidationRequest extends StackEvent {

    @JsonCreator
    public UpscaleStackSaltValidationRequest(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
