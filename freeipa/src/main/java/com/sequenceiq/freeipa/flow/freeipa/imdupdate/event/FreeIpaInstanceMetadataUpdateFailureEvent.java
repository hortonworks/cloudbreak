package com.sequenceiq.freeipa.flow.freeipa.imdupdate.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class FreeIpaInstanceMetadataUpdateFailureEvent extends StackEvent {

    private final Exception exception;

    @JsonCreator
    public FreeIpaInstanceMetadataUpdateFailureEvent(
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
