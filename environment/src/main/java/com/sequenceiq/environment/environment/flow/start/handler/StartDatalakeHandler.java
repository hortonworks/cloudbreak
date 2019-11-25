package com.sequenceiq.environment.environment.flow.start.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.start.event.EnvStartEvent;
import com.sequenceiq.environment.environment.flow.start.event.EnvStartFailedEvent;
import com.sequenceiq.environment.environment.flow.start.event.EnvStartHandlerSelectors;
import com.sequenceiq.environment.environment.flow.start.event.EnvStartStateSelectors;
import com.sequenceiq.environment.environment.service.SdxService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;

@Component
public class StartDatalakeHandler extends EventSenderAwareHandler<EnvironmentDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartDatalakeHandler.class);

    private final SdxService sdxService;

    protected StartDatalakeHandler(EventSender eventSender, SdxService sdxService) {
        super(eventSender);
        this.sdxService = sdxService;
    }

    @Override
    public String selector() {
        return EnvStartHandlerSelectors.START_DATALAKE_HANDLER_EVENT.name();
    }

    @Override
    public void accept(Event<EnvironmentDto> environmentDtoEvent) {
        EnvironmentDto environmentDto = environmentDtoEvent.getData();
        try {
            sdxService.startAttachedDatalake(environmentDto.getId(), environmentDto.getName());
            EnvStartEvent envStartEvent = EnvStartEvent.EnvStartEventBuilder.anEnvStartEvent()
                    .withSelector(EnvStartStateSelectors.ENV_START_DATAHUB_EVENT.selector())
                    .withResourceId(environmentDto.getId())
                    .withResourceName(environmentDto.getName())
                    .build();
            eventSender().sendEvent(envStartEvent, environmentDtoEvent.getHeaders());
        } catch (Exception e) {
            LOGGER.error("Error occurred during starting Datalake(s) for environment. {}", environmentDto, e);
            EnvStartFailedEvent failedEvent = new EnvStartFailedEvent(environmentDto.getId(), environmentDto.getName(), e, environmentDto.getResourceCrn(),
                    EnvironmentStatus.START_DATALAKE_FAILED);
            eventSender().sendEvent(failedEvent, environmentDtoEvent.getHeaders());
        }
    }
}
