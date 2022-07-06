package com.sequenceiq.flow.core.helloworld.flowevents;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.flow.core.helloworld.HelloWorldSelectableEvent;

public class HelloWorldFailedEvent extends HelloWorldSelectableEvent {

    private final Exception exception;

    @JsonCreator
    public HelloWorldFailedEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("exception") Exception exception) {

        super(resourceId);
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }

}
