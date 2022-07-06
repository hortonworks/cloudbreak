package com.sequenceiq.redbeams.flow.redbeams.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RedbeamsFailureEvent extends RedbeamsEvent {

    private final Exception exception;

    public RedbeamsFailureEvent(Long resourceId, Exception exception) {
        super(resourceId);
        this.exception = exception;
    }

    public RedbeamsFailureEvent(Long resourceId, Exception exception, boolean force) {
        super(resourceId, force);
        this.exception = exception;
    }

    @JsonCreator
    public RedbeamsFailureEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("exception") Exception exception,
            @JsonProperty("forced") boolean forced) {

        super(selector, resourceId, forced);
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }

    @Override
    public String toString() {
        return "RedbeamsFailureEvent{" +
                "exception=" + exception +
                "} " + super.toString();
    }
}
