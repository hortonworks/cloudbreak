package com.sequenceiq.environment.environment.flow.creation.event;

import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.FAILED_ENV_CREATION_EVENT;

import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;

public class EnvCreationFailureEvent extends BaseNamedFlowEvent {

    private final Exception exception;

    public EnvCreationFailureEvent(Long environmentId, String resourceName, Exception exception, String resourceCrn) {
        super(FAILED_ENV_CREATION_EVENT.name(), environmentId, resourceName, resourceCrn);
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }
}
