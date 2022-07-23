package com.sequenceiq.environment.environment.flow.start.event;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.CLASS;
import static com.sequenceiq.environment.environment.flow.start.event.EnvStartStateSelectors.FAILED_ENV_START_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.flow.reactor.api.event.BaseFailedFlowEvent;

public class EnvStartFailedEvent extends BaseFailedFlowEvent implements Selectable {

    @JsonTypeInfo(use = CLASS, property = "@type")
    private final Exception exception;

    private final EnvironmentDto environmentDto;

    private final EnvironmentStatus environmentStatus;

    @JsonCreator
    public EnvStartFailedEvent(
            @JsonProperty("environmentDto") EnvironmentDto environmentDto,
            @JsonProperty("exception") Exception exception,
            @JsonProperty("environmentStatus") EnvironmentStatus environmentStatus) {

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
