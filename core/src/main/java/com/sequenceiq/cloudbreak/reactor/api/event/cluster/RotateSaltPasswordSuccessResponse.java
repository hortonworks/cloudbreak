package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class RotateSaltPasswordSuccessResponse extends StackEvent {
    @JsonCreator
    public RotateSaltPasswordSuccessResponse(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
