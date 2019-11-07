package com.sequenceiq.environment.environment.flow.stop.handler;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.stop.event.EnvStopEvent;
import com.sequenceiq.environment.environment.flow.stop.event.EnvStopFailedEvent;
import com.sequenceiq.environment.environment.flow.stop.event.EnvStopHandlerSelectors;
import com.sequenceiq.environment.environment.flow.stop.event.EnvStopStateSelectors;
import com.sequenceiq.environment.environment.service.DistroxService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;

@Component
public class StopDatahubHandler extends EventSenderAwareHandler<EnvironmentDto> {

    private final DistroxService distroxService;

    protected StopDatahubHandler(EventSender eventSender, DistroxService distroxService) {
        super(eventSender);
        this.distroxService = distroxService;
    }

    @Override
    public String selector() {
        return EnvStopHandlerSelectors.STOP_DATAHUB_HANDLER_EVENT.name();
    }

    @Override
    public void accept(Event<EnvironmentDto> environmentDtoEvent) {
        EnvironmentDto environmentDto = environmentDtoEvent.getData();
        try {
            distroxService.stopAttachedDistrox(environmentDto.getId(), environmentDto.getName());
            EnvStopEvent envStopEvent = EnvStopEvent.EnvStopEventBuilder.anEnvStopEvent()
                    .withSelector(EnvStopStateSelectors.ENV_STOP_DATALAKE_EVENT.selector())
                    .withResourceId(environmentDto.getId())
                    .withResourceName(environmentDto.getName())
                    .build();
            eventSender().sendEvent(envStopEvent, environmentDtoEvent.getHeaders());
        } catch (Exception e) {
            EnvStopFailedEvent failedEvent = new EnvStopFailedEvent(environmentDto.getId(), environmentDto.getName(), e, environmentDto.getResourceCrn(),
                    EnvironmentStatus.STOP_DATAHUB_FAILED);
            eventSender().sendEvent(failedEvent, environmentDtoEvent.getHeaders());
        }
    }
}
