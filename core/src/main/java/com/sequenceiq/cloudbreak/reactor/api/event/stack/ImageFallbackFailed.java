package com.sequenceiq.cloudbreak.reactor.api.event.stack;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class ImageFallbackFailed extends StackFailureEvent {

    @JsonCreator
    public ImageFallbackFailed(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception exception) {
        super(stackId, exception);
    }

}
