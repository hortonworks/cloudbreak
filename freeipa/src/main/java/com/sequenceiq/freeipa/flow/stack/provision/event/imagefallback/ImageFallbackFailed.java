package com.sequenceiq.freeipa.flow.stack.provision.event.imagefallback;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;

public class ImageFallbackFailed extends StackFailureEvent {

    @JsonCreator
    public ImageFallbackFailed(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception exception) {
        super(stackId, exception);
    }

}
