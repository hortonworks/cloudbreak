package com.sequenceiq.environment.environment.flow.stop.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.stop.event.EnvStopEvent;
import com.sequenceiq.environment.environment.flow.stop.event.EnvStopFailedEvent;
import com.sequenceiq.environment.environment.flow.stop.event.EnvStopHandlerSelectors;
import com.sequenceiq.environment.environment.flow.stop.event.EnvStopStateSelectors;
import com.sequenceiq.environment.environment.service.SdxService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;

@Component
public class StopDatalakeHandler extends EventSenderAwareHandler<EnvironmentDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StopFreeipaHandler.class);

    private final SdxService sdxService;

    protected StopDatalakeHandler(EventSender eventSender, SdxService sdxService) {
        super(eventSender);
        this.sdxService = sdxService;
    }

    @Override
    public String selector() {
        return EnvStopHandlerSelectors.STOP_DATALAKE_HANDLER_EVENT.name();
    }

    @Override
    public void accept(Event<EnvironmentDto> environmentDtoEvent) {
        EnvironmentDto environmentDto = environmentDtoEvent.getData();
        try {
            sdxService.stopAttachedDatalakeClusters(environmentDto.getId(), environmentDto.getName());
            EnvStopEvent envStopEvent = EnvStopEvent.EnvStopEventBuilder.anEnvStopEvent()
                    .withSelector(EnvStopStateSelectors.ENV_STOP_FREEIPA_EVENT.selector())
                    .withResourceId(environmentDto.getId())
                    .withResourceName(environmentDto.getName())
                    .build();
            eventSender().sendEvent(envStopEvent, environmentDtoEvent.getHeaders());
        } catch (Exception e) {
            LOGGER.error("Error occurred during stopping Datalake(s) for environment. {}", environmentDto, e);
            EnvStopFailedEvent failedEvent = new EnvStopFailedEvent(environmentDto.getId(), environmentDto.getName(), e, environmentDto.getResourceCrn(),
                    EnvironmentStatus.STOP_DATALAKE_FAILED);
            eventSender().sendEvent(failedEvent, environmentDtoEvent.getHeaders());
        }
    }
}
