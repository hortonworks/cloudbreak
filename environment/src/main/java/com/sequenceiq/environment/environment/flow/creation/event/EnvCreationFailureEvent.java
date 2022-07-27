package com.sequenceiq.environment.environment.flow.creation.event;

import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.FAILED_ENV_CREATION_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;

public class EnvCreationFailureEvent extends BaseNamedFlowEvent {

    private final Exception exception;

    @JsonCreator
    public EnvCreationFailureEvent(
            @JsonProperty("resourceId") Long environmentId,
            @JsonProperty("resourceName") String resourceName,
            @JsonProperty("exception") Exception exception,
            @JsonProperty("resourceCrn") String resourceCrn) {

        super(FAILED_ENV_CREATION_EVENT.name(), environmentId, resourceName, resourceCrn);
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }
}
