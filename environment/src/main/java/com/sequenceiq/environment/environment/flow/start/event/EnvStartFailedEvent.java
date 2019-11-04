package com.sequenceiq.environment.environment.flow.start.event;

import static com.sequenceiq.environment.environment.flow.start.event.EnvStartStateSelectors.FAILED_ENV_START_EVENT;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;

public class EnvStartFailedEvent extends BaseNamedFlowEvent implements Selectable {

    private final Exception exception;

    private final EnvironmentStatus environmentStatus;

    public EnvStartFailedEvent(Long environmentId, String resourceName, Exception exception, String resourceCrn, EnvironmentStatus environmentStatus) {
        super(FAILED_ENV_START_EVENT.name(), environmentId, null, resourceName, resourceCrn);
        this.exception = exception;
        this.environmentStatus = environmentStatus;
    }

    @Override
    public String selector() {
        return FAILED_ENV_START_EVENT.name();
    }

    public Exception getException() {
        return exception;
    }

    public EnvironmentStatus getEnvironmentStatus() {
        return environmentStatus;
    }
}
