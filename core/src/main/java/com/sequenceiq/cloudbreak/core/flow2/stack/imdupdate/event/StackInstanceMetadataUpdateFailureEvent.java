package com.sequenceiq.cloudbreak.core.flow2.stack.imdupdate.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class StackInstanceMetadataUpdateFailureEvent extends StackEvent {

    private final Exception exception;

    @JsonCreator
    public StackInstanceMetadataUpdateFailureEvent(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception exception) {
        super(stackId);
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }

    @Override
    public String toString() {
        return super.toString() + "FreeIpaInstanceMetadataUpdateFailureEvent{" +
                "exception=" + exception +
                '}';
    }
}
