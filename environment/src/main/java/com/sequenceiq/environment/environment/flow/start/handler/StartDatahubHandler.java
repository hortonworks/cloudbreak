package com.sequenceiq.environment.environment.flow.start.handler;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.start.event.EnvStartEvent;
import com.sequenceiq.environment.environment.flow.start.event.EnvStartFailedEvent;
import com.sequenceiq.environment.environment.flow.start.event.EnvStartHandlerSelectors;
import com.sequenceiq.environment.environment.flow.start.event.EnvStartStateSelectors;
import com.sequenceiq.environment.environment.service.DistroxService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;

@Component
public class StartDatahubHandler extends EventSenderAwareHandler<EnvironmentDto> {

    private final DistroxService distroxService;

    protected StartDatahubHandler(EventSender eventSender, DistroxService distroxService) {
        super(eventSender);
        this.distroxService = distroxService;
    }

    @Override
    public String selector() {
        return EnvStartHandlerSelectors.START_DATAHUB_HANDLER_EVENT.name();
    }

    @Override
    public void accept(Event<EnvironmentDto> environmentDtoEvent) {
        EnvironmentDto environmentDto = environmentDtoEvent.getData();
        try {
            distroxService.startAttachedDistrox(environmentDto.getId(), environmentDto.getName());
            EnvStartEvent envStartEvent = EnvStartEvent.EnvStartEventBuilder.anEnvStartEvent()
                    .withSelector(EnvStartStateSelectors.FINISH_ENV_START_EVENT.selector())
                    .withResourceId(environmentDto.getId())
                    .withResourceName(environmentDto.getName())
                    .build();
            eventSender().sendEvent(envStartEvent, environmentDtoEvent.getHeaders());
        } catch (Exception e) {
            EnvStartFailedEvent failedEvent = new EnvStartFailedEvent(environmentDto.getId(), environmentDto.getName(), e, environmentDto.getResourceCrn(),
                    EnvironmentStatus.START_DATAHUB_FAILED);
            eventSender().sendEvent(failedEvent, environmentDtoEvent.getHeaders());
        }
    }
}
