package com.sequenceiq.environment.environment.flow.stop.handler;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.stop.event.EnvStopEvent;
import com.sequenceiq.environment.environment.flow.stop.event.EnvStopFailedEvent;
import com.sequenceiq.environment.environment.flow.stop.event.EnvStopHandlerSelectors;
import com.sequenceiq.environment.environment.flow.stop.event.EnvStopStateSelectors;
import com.sequenceiq.environment.environment.service.sdx.SdxPollerService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

@Component
public class StopDatalakeHandler extends EventSenderAwareHandler<EnvironmentDto> {

    private final SdxPollerService sdxPollerService;

    protected StopDatalakeHandler(EventSender eventSender, SdxPollerService sdxPollerService) {
        super(eventSender);
        this.sdxPollerService = sdxPollerService;
    }

    @Override
    public String selector() {
        return EnvStopHandlerSelectors.STOP_DATALAKE_HANDLER_EVENT.name();
    }

    @Override
    public void accept(Event<EnvironmentDto> environmentDtoEvent) {
        EnvironmentDto environmentDto = environmentDtoEvent.getData();
        try {
            sdxPollerService.stopAttachedDatalakeClusters(environmentDto.getId(), environmentDto.getName());
            EnvStopEvent envStopEvent = EnvStopEvent.builder()
                    .withSelector(EnvStopStateSelectors.ENV_STOP_FREEIPA_EVENT.selector())
                    .withResourceId(environmentDto.getId())
                    .withResourceName(environmentDto.getName())
                    .build();
            eventSender().sendEvent(envStopEvent, environmentDtoEvent.getHeaders());
        } catch (Exception e) {
            EnvStopFailedEvent failedEvent = new EnvStopFailedEvent(environmentDto, e, EnvironmentStatus.STOP_DATALAKE_FAILED);
            eventSender().sendEvent(failedEvent, environmentDtoEvent.getHeaders());
        }
    }
}
