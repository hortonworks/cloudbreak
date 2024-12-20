package com.sequenceiq.environment.environment.flow.start.handler.sdx;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.sdx.common.PlatformAwareSdxConnector;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentStartDto;
import com.sequenceiq.environment.environment.flow.start.event.EnvStartEvent;
import com.sequenceiq.environment.environment.flow.start.event.EnvStartFailedEvent;
import com.sequenceiq.environment.environment.flow.start.event.EnvStartHandlerSelectors;
import com.sequenceiq.environment.environment.flow.start.event.EnvStartStateSelectors;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

@Component
public class StartDatalakeHandler extends EventSenderAwareHandler<EnvironmentStartDto> {

    private final PlatformAwareSdxConnector platformAwareSdxConnector;

    protected StartDatalakeHandler(EventSender eventSender, PlatformAwareSdxConnector platformAwareSdxConnector) {
        super(eventSender);
        this.platformAwareSdxConnector = platformAwareSdxConnector;
    }

    @Override
    public String selector() {
        return EnvStartHandlerSelectors.START_DATALAKE_HANDLER_EVENT.name();
    }

    @Override
    public void accept(Event<EnvironmentStartDto> environmentStartDtoEvent) {
        EnvironmentDto environmentDto = environmentStartDtoEvent.getData().getEnvironmentDto();
        try {
            platformAwareSdxConnector.startByEnvironment(environmentDto.getResourceCrn());
            EnvStartEvent envStartEvent = EnvStartEvent.builder()
                    .withSelector(EnvStartStateSelectors.ENV_START_DATAHUB_EVENT.selector())
                    .withResourceId(environmentDto.getId())
                    .withResourceName(environmentDto.getName())
                    .withDataHubStartAction(environmentStartDtoEvent.getData().getDataHubStart())
                    .build();
            eventSender().sendEvent(envStartEvent, environmentStartDtoEvent.getHeaders());
        } catch (Exception e) {
            EnvStartFailedEvent failedEvent = new EnvStartFailedEvent(environmentDto, e, EnvironmentStatus.START_DATALAKE_FAILED);
            eventSender().sendEvent(failedEvent, environmentStartDtoEvent.getHeaders());
        }
    }
}
