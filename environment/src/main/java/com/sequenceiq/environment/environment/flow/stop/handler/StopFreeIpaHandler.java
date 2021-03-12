package com.sequenceiq.environment.environment.flow.stop.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.stop.event.EnvStopEvent;
import com.sequenceiq.environment.environment.flow.stop.event.EnvStopFailedEvent;
import com.sequenceiq.environment.environment.flow.stop.event.EnvStopHandlerSelectors;
import com.sequenceiq.environment.environment.flow.stop.event.EnvStopStateSelectors;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaPollerService;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.environment.exception.FreeIpaOperationFailedException;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;

@Component
public class StopFreeIpaHandler extends EventSenderAwareHandler<EnvironmentDto> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StopFreeIpaHandler.class);

    private final FreeIpaPollerService freeIpaPollerService;

    private final FreeIpaService freeIpaService;

    protected StopFreeIpaHandler(EventSender eventSender, FreeIpaPollerService freeIpaPollerService, FreeIpaService freeIpaService) {
        super(eventSender);
        this.freeIpaPollerService = freeIpaPollerService;
        this.freeIpaService = freeIpaService;
    }

    @Override
    public String selector() {
        return EnvStopHandlerSelectors.STOP_FREEIPA_HANDLER_EVENT.name();
    }

    @Override
    public void accept(Event<EnvironmentDto> environmentDtoEvent) {
        EnvironmentDto environmentDto = environmentDtoEvent.getData();
        try {
            if (Strings.isNullOrEmpty(environmentDto.getParentEnvironmentCrn())) {
                freeIpaService.describe(environmentDto.getResourceCrn()).ifPresent(freeIpa -> {
                    if (freeIpa.getStatus() != null && !freeIpa.getStatus().isStoppable()) {
                        throw new FreeIpaOperationFailedException("FreeIPA is not in a stoppable state! Current state is: " + freeIpa.getStatus().name());
                    }
                });
                freeIpaPollerService.stopAttachedFreeipaInstances(environmentDto.getId(), environmentDto.getResourceCrn());
            }
            EnvStopEvent envStopEvent = EnvStopEvent.EnvStopEventBuilder.anEnvStopEvent()
                    .withSelector(EnvStopStateSelectors.FINISH_ENV_STOP_EVENT.selector())
                    .withResourceId(environmentDto.getId())
                    .withResourceName(environmentDto.getName())
                    .build();
            eventSender().sendEvent(envStopEvent, environmentDtoEvent.getHeaders());
        } catch (Exception e) {
            LOGGER.warn("Failed to stop Freeipa.", e);
            EnvStopFailedEvent failedEvent = new EnvStopFailedEvent(environmentDto, e, EnvironmentStatus.STOP_FREEIPA_FAILED);
            eventSender().sendEvent(failedEvent, environmentDtoEvent.getHeaders());
        }
    }

}
