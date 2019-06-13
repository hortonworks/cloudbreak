package com.sequenceiq.environment.environment.flow.creation.event;

import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.FAILED_ENV_CREATION_EVENT;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;

public class EnvCreationFailureEvent extends BaseNamedFlowEvent implements Selectable {

    private final Exception exception;

    public EnvCreationFailureEvent(Long environmentId, String resourceName, Exception exception) {
        super(FAILED_ENV_CREATION_EVENT.name(), environmentId, resourceName);
        this.exception = exception;
    }

    @Override
    public String selector() {
        return FAILED_ENV_CREATION_EVENT.name();
    }

    public Exception getException() {
        return exception;
    }
}

