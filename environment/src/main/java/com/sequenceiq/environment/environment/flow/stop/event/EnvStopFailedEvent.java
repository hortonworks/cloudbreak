package com.sequenceiq.environment.environment.flow.stop.event;

import static com.sequenceiq.environment.environment.flow.stop.event.EnvStopStateSelectors.FAILED_ENV_STOP_EVENT;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.flow.reactor.api.event.BaseFailedFlowEvent;

public class EnvStopFailedEvent extends BaseFailedFlowEvent implements Selectable {

    private final EnvironmentDto environmentDto;

    private final EnvironmentStatus environmentStatus;

    public EnvStopFailedEvent(EnvironmentDto environmentDto, Exception exception, EnvironmentStatus environmentStatus) {
        super(FAILED_ENV_STOP_EVENT.name(), environmentDto.getResourceId(),
                environmentDto.getName(), environmentDto.getResourceCrn(), exception);
        this.environmentDto = environmentDto;
        this.environmentStatus = environmentStatus;
    }

    @Override
    public String selector() {
        return FAILED_ENV_STOP_EVENT.name();
    }

    public EnvironmentDto getEnvironmentDto() {
        return environmentDto;
    }

    public EnvironmentStatus getEnvironmentStatus() {
        return environmentStatus;
    }
}
