package com.sequenceiq.freeipa.flow.stack;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.CLASS;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

public class StackFailureEvent extends StackEvent {

    @JsonTypeInfo(use = CLASS, property = "@type")
    private final Exception exception;

    public StackFailureEvent(Long stackId, Exception exception) {
        super(stackId);
        this.exception = exception;
    }

    @JsonCreator
    public StackFailureEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception exception) {
        super(selector, stackId);
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }
}
