package com.sequenceiq.cloudbreak.core.flow2.cluster.java;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class SetDefaultJavaVersionFailedEvent extends StackFailureEvent {

    private final Exception errorDetails;

    @JsonCreator
    public SetDefaultJavaVersionFailedEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("errorDetails") Exception errorDetails) {
        super(selector, resourceId, errorDetails);
        this.errorDetails = errorDetails;
    }

    public Exception getErrorDetails() {
        return errorDetails;
    }

    public String toString() {
        return new StringJoiner(", ", SetDefaultJavaVersionFailedEvent.class.getSimpleName() + "[", "]")
                .add("errorDetails=" + errorDetails)
                .toString();
    }

}
