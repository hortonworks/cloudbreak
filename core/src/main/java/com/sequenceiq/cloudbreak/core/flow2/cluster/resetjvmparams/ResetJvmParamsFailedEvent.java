package com.sequenceiq.cloudbreak.core.flow2.cluster.resetjvmparams;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class ResetJvmParamsFailedEvent extends StackFailureEvent {

    private final Exception errorDetails;

    @JsonCreator
    public ResetJvmParamsFailedEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("errorDetails") Exception errorDetails) {
        super(selector, resourceId, errorDetails);
        this.errorDetails = errorDetails;
    }

    public Exception getErrorDetails() {
        return errorDetails;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ResetJvmParamsFailedEvent.class.getSimpleName() + "[", "]")
                .add("errorDetails=" + errorDetails)
                .toString();
    }
}
