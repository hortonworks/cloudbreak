package com.sequenceiq.cloudbreak.reactor.api.event.cluster.atlas;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CheckAtlasUpdatedSaltFailedEvent extends CheckAtlasUpdatedStackEvent {
    private final Exception exception;

    @JsonCreator
    public CheckAtlasUpdatedSaltFailedEvent(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception exception) {
        super(null, stackId);
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }
}
