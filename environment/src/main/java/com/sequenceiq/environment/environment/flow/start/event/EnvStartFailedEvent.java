package com.sequenceiq.environment.environment.flow.start.event;

import static com.sequenceiq.environment.environment.flow.start.event.EnvStartStateSelectors.FAILED_ENV_START_EVENT;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.flow.reactor.api.event.BaseFailedFlowEvent;

public class EnvStartFailedEvent extends BaseFailedFlowEvent implements Selectable {

    private final Exception exception;

    private final EnvironmentDto environmentDto;

    private final EnvironmentStatus environmentStatus;

    public EnvStartFailedEvent(EnvironmentDto environmentDto, Exception exception, EnvironmentStatus environmentStatus) {
        super(FAILED_ENV_START_EVENT.name(), environmentDto.getResourceId(), null,
                environmentDto.getName(), environmentDto.getResourceCrn(), exception);
        this.exception = exception;
        this.environmentDto = environmentDto;
        this.environmentStatus = environmentStatus;
    }

    @Override
    public String selector() {
        return FAILED_ENV_START_EVENT.name();
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
