package com.sequenceiq.cloudbreak.reactor.api.event.stack;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ImageFallbackRequest extends StackEvent {

    @JsonCreator
    public ImageFallbackRequest(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }

}
