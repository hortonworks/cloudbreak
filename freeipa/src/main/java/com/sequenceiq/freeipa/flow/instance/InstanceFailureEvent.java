package com.sequenceiq.freeipa.flow.instance;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class InstanceFailureEvent extends InstanceEvent {

    private final Exception exception;

    @JsonCreator
    public InstanceFailureEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("exception") Exception exception,
            @JsonProperty("instanceIds") List<String> instanceIds) {
        super(null, resourceId, instanceIds);
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }

}
