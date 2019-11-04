package com.sequenceiq.environment.environment.flow.start.handler;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.start.event.EnvStartEvent;
import com.sequenceiq.environment.environment.flow.start.event.EnvStartFailedEvent;
import com.sequenceiq.environment.environment.flow.start.event.EnvStartHandlerSelectors;
import com.sequenceiq.environment.environment.flow.start.event.EnvStartStateSelectors;
import com.sequenceiq.environment.environment.service.FreeipaService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;

@Component
public class StartFreeipaHandler extends EventSenderAwareHandler<EnvironmentDto> {

    private final FreeipaService freeipaService;

    protected StartFreeipaHandler(EventSender eventSender, FreeipaService freeipaService) {
        super(eventSender);
        this.freeipaService = freeipaService;
    }

    @Override
    public String selector() {
        return EnvStartHandlerSelectors.START_FREEIPA_HANDLER_EVENT.name();
    }

    @Override
    public void accept(Event<EnvironmentDto> environmentDtoEvent) {
        EnvironmentDto environmentDto = environmentDtoEvent.getData();
        try {
            freeipaService.startAttachedFreeipa(environmentDto.getResourceCrn());
            EnvStartEvent envStartEvent = EnvStartEvent.EnvStartEventBuilder.anEnvStartEvent()
                    .withSelector(EnvStartStateSelectors.ENV_START_DATALAKE_EVENT.selector())
                    .withResourceId(environmentDto.getId())
                    .withResourceName(environmentDto.getName())
                    .build();
            eventSender().sendEvent(envStartEvent, environmentDtoEvent.getHeaders());
        } catch (Exception e) {
            EnvStartFailedEvent failedEvent = new EnvStartFailedEvent(environmentDto.getId(), environmentDto.getName(), e, environmentDto.getResourceCrn(),
                    EnvironmentStatus.START_FREEIPA_FAILED);
            eventSender().sendEvent(failedEvent, environmentDtoEvent.getHeaders());
        }
    }
}
