package com.sequenceiq.environment.environment.flow.deletion.handler;

import com.sequenceiq.environment.environment.dto.EnvironmentDeletionDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;

import reactor.bus.Event;

public class HandlerFailureConjoiner {

    private final Exception exception;

    private final EnvironmentDeletionDto environmentDeletionDto;

    private final EnvironmentDto environmentDto;

    private final EnvDeleteEvent envDeleteEvent;

    private final Event<EnvironmentDeletionDto> environmentDtoEvent;

    public HandlerFailureConjoiner(Exception exception, Event<EnvironmentDeletionDto> environmentDtoEvent, EnvDeleteEvent envDeleteEvent) {
        this.environmentDto = environmentDtoEvent.getData().getEnvironmentDto();
        this.environmentDeletionDto = environmentDtoEvent.getData();
        this.environmentDtoEvent = environmentDtoEvent;
        this.envDeleteEvent = envDeleteEvent;
        this.exception = exception;
    }

    public EnvDeleteEvent getEnvDeleteEvent() {
        return envDeleteEvent;
    }

    public Exception getException() {
        return exception;
    }

    public EnvironmentDto getEnvironmentDto() {
        return environmentDto;
    }

    public EnvironmentDeletionDto getEnvironmentDeletionDto() {
        return environmentDeletionDto;
    }

    public Event<EnvironmentDeletionDto> getEnvironmentDtoEvent() {
        return environmentDtoEvent;
    }

}
