package com.sequenceiq.environment.environment.flow.delete.event;

import static com.sequenceiq.environment.environment.flow.delete.event.EnvDeleteStateSelectors.FAILED_ENV_DELETE_EVENT;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;

public class EnvDeleteFailedEvent extends BaseNamedFlowEvent implements Selectable {

    private final Exception exception;

    public EnvDeleteFailedEvent(Long environmentId, String resourceName, Exception exception) {
        super(FAILED_ENV_DELETE_EVENT.name(), environmentId, resourceName);
        this.exception = exception;
    }

    @Override
    public String selector() {
        return FAILED_ENV_DELETE_EVENT.name();
    }

    public Exception getException() {
        return exception;
    }
}
