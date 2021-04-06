package com.sequenceiq.environment.environment.flow.start.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentStartDto;
import com.sequenceiq.environment.environment.flow.start.event.EnvStartEvent;
import com.sequenceiq.environment.environment.flow.start.event.EnvStartFailedEvent;
import com.sequenceiq.environment.environment.flow.start.event.EnvStartHandlerSelectors;
import com.sequenceiq.environment.environment.flow.start.event.EnvStartStateSelectors;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaPollerService;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.environment.exception.FreeIpaOperationFailedException;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;

@Component
public class StartFreeIpaHandler extends EventSenderAwareHandler<EnvironmentStartDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartFreeIpaHandler.class);

    private final FreeIpaPollerService freeIpaPollerService;

    private final FreeIpaService freeIpaService;

    protected StartFreeIpaHandler(EventSender eventSender, FreeIpaPollerService freeIpaPollerService, FreeIpaService freeIpaService) {
        super(eventSender);
        this.freeIpaPollerService = freeIpaPollerService;
        this.freeIpaService = freeIpaService;
    }

    @Override
    public String selector() {
        return EnvStartHandlerSelectors.START_FREEIPA_HANDLER_EVENT.name();
    }

    @Override
    public void accept(Event<EnvironmentStartDto> environmentStartDtoEvent) {
        EnvironmentDto environmentDto = environmentStartDtoEvent.getData().getEnvironmentDto();
        try {
            freeIpaService.describe(environmentDto.getResourceCrn()).ifPresentOrElse(freeIpa -> {
                if (freeIpa.getStatus() == null) {
                    throw new FreeIpaOperationFailedException("FreeIPA status is unpredictable, env start will be interrupted.");
                } else if (freeIpa.getStatus().isAvailable() || freeIpa.getStatus().isStartInProgressPhase()) {
                    LOGGER.info("Start has already been triggered continuing without new start trigger. FreeIPA status: {}", freeIpa.getStatus());
                } else if (!freeIpa.getStatus().isStartable()) {
                    throw new FreeIpaOperationFailedException("FreeIPA is not in a valid state to start! Current state is: " + freeIpa.getStatus().name());
                } else {
                    LOGGER.info("FreeIPA will be started.");
                    freeIpaPollerService.startAttachedFreeipaInstances(environmentDto.getId(), environmentDto.getResourceCrn());
                }
            }, () -> LOGGER.info("FreeIPA cannot be found by environment crn"));
            EnvStartEvent envStartEvent = EnvStartEvent.EnvStartEventBuilder.anEnvStartEvent()
                    .withSelector(EnvStartStateSelectors.ENV_START_DATALAKE_EVENT.selector())
                    .withResourceId(environmentDto.getId())
                    .withResourceName(environmentDto.getName())
                    .withDataHubStart(environmentStartDtoEvent.getData().getDataHubStart())
                    .build();
            eventSender().sendEvent(envStartEvent, environmentStartDtoEvent.getHeaders());
        } catch (Exception e) {
            LOGGER.warn("Failed to start Freeipa.", e);
            EnvStartFailedEvent failedEvent = new EnvStartFailedEvent(environmentDto, e, EnvironmentStatus.START_FREEIPA_FAILED);
            eventSender().sendEvent(failedEvent, environmentStartDtoEvent.getHeaders());
        }
    }

}
