package com.sequenceiq.environment.environment.flow.start.handler;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentStartDto;
import com.sequenceiq.environment.environment.flow.start.event.EnvStartEvent;
import com.sequenceiq.environment.environment.flow.start.event.EnvStartFailedEvent;
import com.sequenceiq.environment.environment.flow.start.event.EnvStartHandlerSelectors;
import com.sequenceiq.environment.environment.flow.start.event.EnvStartStateSelectors;
import com.sequenceiq.environment.environment.service.sdx.SdxPollerService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;

@Component
public class StartDatalakeHandler extends EventSenderAwareHandler<EnvironmentStartDto> {

    private final SdxPollerService sdxPollerService;

    protected StartDatalakeHandler(EventSender eventSender, SdxPollerService sdxPollerService) {
        super(eventSender);
        this.sdxPollerService = sdxPollerService;
    }

    @Override
    public String selector() {
        return EnvStartHandlerSelectors.START_DATALAKE_HANDLER_EVENT.name();
    }

    @Override
    public void accept(Event<EnvironmentStartDto> environmentStartDtoEvent) {
        //TODO [AF]: test is missing
        EnvironmentDto environmentDto = environmentStartDtoEvent.getData().getEnvironmentDto();
        try {
            sdxPollerService.startAttachedDatalake(environmentDto.getId(), environmentDto.getName());
            EnvStartEvent envStartEvent = EnvStartEvent.EnvStartEventBuilder.anEnvStartEvent()
                    .withSelector(EnvStartStateSelectors.ENV_START_DATAHUB_EVENT.selector())
                    .withResourceId(environmentDto.getId())
                    .withResourceName(environmentDto.getName())
                    .withDataHubStart(environmentStartDtoEvent.getData().getDataHubStart())
                    .build();
            eventSender().sendEvent(envStartEvent, environmentStartDtoEvent.getHeaders());
        } catch (Exception e) {
            EnvStartFailedEvent failedEvent = new EnvStartFailedEvent(environmentDto, e, EnvironmentStatus.START_DATALAKE_FAILED);
            eventSender().sendEvent(failedEvent, environmentStartDtoEvent.getHeaders());
        }
    }
}
