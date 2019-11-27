package com.sequenceiq.environment.environment.flow.stop.event;

import static com.sequenceiq.environment.environment.flow.stop.event.EnvStopStateSelectors.FAILED_ENV_STOP_EVENT;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;

public class EnvStopFailedEvent extends BaseNamedFlowEvent implements Selectable {

    private final Exception exception;

    private final EnvironmentDto environmentDto;

    private final EnvironmentStatus environmentStatus;

    public EnvStopFailedEvent(EnvironmentDto environmentDto, Exception exception, EnvironmentStatus environmentStatus) {
        super(FAILED_ENV_STOP_EVENT.name(), environmentDto.getResourceId(), environmentDto.getName(), environmentDto.getResourceCrn());
        this.exception = exception;
        this.environmentDto = environmentDto;
        this.environmentStatus = environmentStatus;
    }

    @Override
    public String selector() {
        return FAILED_ENV_STOP_EVENT.name();
    }

    public Exception getException() {
        return exception;
    }

    public EnvironmentDto getEnvironmentDto() {
        return environmentDto;
    }

    public EnvironmentStatus getEnvironmentStatus() {
        return environmentStatus;
    }
}
