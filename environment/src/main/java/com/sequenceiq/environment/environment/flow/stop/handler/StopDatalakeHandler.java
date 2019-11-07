package com.sequenceiq.environment.environment.flow.stop.handler;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.stop.event.EnvStopEvent;
import com.sequenceiq.environment.environment.flow.stop.event.EnvStopFailedEvent;
import com.sequenceiq.environment.environment.flow.stop.event.EnvStopHandlerSelectors;
import com.sequenceiq.environment.environment.flow.stop.event.EnvStopStateSelectors;
import com.sequenceiq.environment.environment.service.DatalakeService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;

@Component
public class StopDatalakeHandler extends EventSenderAwareHandler<EnvironmentDto> {

    private final DatalakeService datalakeService;

    protected StopDatalakeHandler(EventSender eventSender, DatalakeService datalakeService) {
        super(eventSender);
        this.datalakeService = datalakeService;
    }

    @Override
    public String selector() {
        return EnvStopHandlerSelectors.STOP_DATALAKE_HANDLER_EVENT.name();
    }

    @Override
    public void accept(Event<EnvironmentDto> environmentDtoEvent) {
        EnvironmentDto environmentDto = environmentDtoEvent.getData();
        try {
            datalakeService.stopAttachedDatalake(environmentDto.getId(), environmentDto.getName());
            EnvStopEvent envStopEvent = EnvStopEvent.EnvStopEventBuilder.anEnvStopEvent()
                    .withSelector(EnvStopStateSelectors.ENV_STOP_FREEIPA_EVENT.selector())
                    .withResourceId(environmentDto.getId())
                    .withResourceName(environmentDto.getName())
                    .build();
            eventSender().sendEvent(envStopEvent, environmentDtoEvent.getHeaders());
        } catch (Exception e) {
            EnvStopFailedEvent failedEvent = new EnvStopFailedEvent(environmentDto.getId(), environmentDto.getName(), e, environmentDto.getResourceCrn(),
                    EnvironmentStatus.STOP_DATALAKE_FAILED);
            eventSender().sendEvent(failedEvent, environmentDtoEvent.getHeaders());
        }
    }
}
