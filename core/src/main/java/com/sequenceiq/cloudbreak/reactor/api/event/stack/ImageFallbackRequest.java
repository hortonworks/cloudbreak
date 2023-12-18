package com.sequenceiq.cloudbreak.reactor.api.event.stack;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ImageFallbackRequest extends StackEvent {

    private final CloudContext cloudContext;

    @JsonCreator
    public ImageFallbackRequest(
            @JsonProperty("resourceId") Long stackId, @JsonProperty("cloudContext") CloudContext cloudContext) {
        super(stackId);
        this.cloudContext = cloudContext;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }
}
