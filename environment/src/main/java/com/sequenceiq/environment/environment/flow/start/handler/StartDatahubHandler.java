package com.sequenceiq.environment.environment.flow.start.handler;

import static com.sequenceiq.common.api.type.DataHubStartAction.START_ALL;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentStartDto;
import com.sequenceiq.environment.environment.flow.start.event.EnvStartEvent;
import com.sequenceiq.environment.environment.flow.start.event.EnvStartFailedEvent;
import com.sequenceiq.environment.environment.flow.start.event.EnvStartHandlerSelectors;
import com.sequenceiq.environment.environment.flow.start.event.EnvStartStateSelectors;
import com.sequenceiq.environment.environment.service.datahub.DatahubPollerService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;

@Component
public class StartDatahubHandler extends EventSenderAwareHandler<EnvironmentStartDto> {

    private final DatahubPollerService datahubPollerService;

    protected StartDatahubHandler(EventSender eventSender, DatahubPollerService datahubPollerService) {
        super(eventSender);
        this.datahubPollerService = datahubPollerService;
    }

    @Override
    public String selector() {
        return EnvStartHandlerSelectors.START_DATAHUB_HANDLER_EVENT.name();
    }

    @Override
    public void accept(Event<EnvironmentStartDto> environmentStartDtoEvent) {
        EnvironmentDto environmentDto = environmentStartDtoEvent.getData().getEnvironmentDto();
        try {
            if (START_ALL == environmentStartDtoEvent.getData().getDataHubStart()) {
                datahubPollerService.startAttachedDatahubClusters(environmentDto.getId(), environmentDto.getResourceCrn());
            }
            EnvStartEvent envStartEvent = EnvStartEvent.EnvStartEventBuilder.anEnvStartEvent()
                    .withSelector(EnvStartStateSelectors.ENV_START_SYNCHRONIZE_USERS_EVENT.selector())
                    .withResourceId(environmentDto.getId())
                    .withResourceName(environmentDto.getName())
                    .withDataHubStart(environmentStartDtoEvent.getData().getDataHubStart())
                    .build();
            eventSender().sendEvent(envStartEvent, environmentStartDtoEvent.getHeaders());
        } catch (Exception e) {
            EnvStartFailedEvent failedEvent = new EnvStartFailedEvent(environmentDto, e, EnvironmentStatus.START_DATAHUB_FAILED);
            eventSender().sendEvent(failedEvent, environmentStartDtoEvent.getHeaders());
        }
    }
}
