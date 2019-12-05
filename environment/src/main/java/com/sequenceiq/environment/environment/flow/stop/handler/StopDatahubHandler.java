package com.sequenceiq.environment.environment.flow.stop.handler;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.stop.event.EnvStopEvent;
import com.sequenceiq.environment.environment.flow.stop.event.EnvStopFailedEvent;
import com.sequenceiq.environment.environment.flow.stop.event.EnvStopHandlerSelectors;
import com.sequenceiq.environment.environment.flow.stop.event.EnvStopStateSelectors;
import com.sequenceiq.environment.environment.service.datahub.DatahubPollerService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;

@Component
public class StopDatahubHandler extends EventSenderAwareHandler<EnvironmentDto> {

    private final DatahubPollerService datahubPollerService;

    protected StopDatahubHandler(EventSender eventSender, DatahubPollerService datahubPollerService) {
        super(eventSender);
        this.datahubPollerService = datahubPollerService;
    }

    @Override
    public String selector() {
        return EnvStopHandlerSelectors.STOP_DATAHUB_HANDLER_EVENT.name();
    }

    @Override
    public void accept(Event<EnvironmentDto> environmentDtoEvent) {
        EnvironmentDto environmentDto = environmentDtoEvent.getData();
        try {
            datahubPollerService.stopAttachedDatahubClusters(environmentDto.getId(), environmentDto.getResourceCrn());
            EnvStopEvent envStopEvent = EnvStopEvent.EnvStopEventBuilder.anEnvStopEvent()
                    .withSelector(EnvStopStateSelectors.ENV_STOP_DATALAKE_EVENT.selector())
                    .withResourceId(environmentDto.getId())
                    .withResourceName(environmentDto.getName())
                    .build();
            eventSender().sendEvent(envStopEvent, environmentDtoEvent.getHeaders());
        } catch (Exception e) {
            EnvStopFailedEvent failedEvent = new EnvStopFailedEvent(environmentDto, e, EnvironmentStatus.STOP_DATAHUB_FAILED);
            eventSender().sendEvent(failedEvent, environmentDtoEvent.getHeaders());
        }
    }
}
