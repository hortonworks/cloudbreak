package com.sequenceiq.freeipa.flow.instance;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.CLASS;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

public class InstanceFailureEvent extends InstanceEvent {

    @JsonTypeInfo(use = CLASS, property = "@type")
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
