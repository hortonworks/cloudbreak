package com.sequenceiq.environment.environment.flow.stop.event;

import static com.sequenceiq.environment.environment.flow.stop.event.EnvStopStateSelectors.FAILED_ENV_STOP_EVENT;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;

public class EnvStopFailedEvent extends BaseNamedFlowEvent implements Selectable {

    private final Exception exception;

    private final EnvironmentStatus environmentStatus;

    public EnvStopFailedEvent(Long environmentId, String resourceName, Exception exception, String resourceCrn, EnvironmentStatus environmentStatus) {
        super(FAILED_ENV_STOP_EVENT.name(), environmentId, resourceName, resourceCrn);
        this.exception = exception;
        this.environmentStatus = environmentStatus;
    }

    @Override
    public String selector() {
        return FAILED_ENV_STOP_EVENT.name();
    }

    public Exception getException() {
        return exception;
    }

    public EnvironmentStatus getEnvironmentStatus() {
        return environmentStatus;
    }
}
