package com.sequenceiq.externalizedcompute.flow;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class ExternalizedComputeClusterFailedEvent extends ExternalizedComputeClusterEvent {

    private final Exception exception;

    @JsonCreator
    public ExternalizedComputeClusterFailedEvent(@JsonProperty("resourceId") Long externalizedComputeClusterId, @JsonProperty("actorCrn") String actorCrn,
            @JsonProperty("exception") Exception exception) {
        super(externalizedComputeClusterId, actorCrn);
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }

    @Override
    public String toString() {
        return "ExternalizedComputeClusterFailedEvent{" +
                "exception=" + exception +
                "} " + super.toString();
    }
}