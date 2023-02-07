package com.sequenceiq.environment.environment.flow.start.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.wiam.client.GrpcWiamClient;
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

@Component
public class SynchronizeUsersHandler extends EventSenderAwareHandler<EnvironmentStartDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SynchronizeUsersHandler.class);

    private final FreeIpaPollerService freeIpaPollerService;

    private final FreeIpaService freeIpaService;

    private final GrpcWiamClient wiamClient;

    private final EntitlementService entitlementService;

    @Value("${environment.freeipa.synchronizeOnStart:}")
    private boolean synchronizeOnStartEnabled;

    protected SynchronizeUsersHandler(EventSender eventSender, FreeIpaPollerService freeIpaPollerService, FreeIpaService freeIpaService,
            GrpcWiamClient wiamClient, EntitlementService entitlementService) {
        super(eventSender);
        this.freeIpaPollerService = freeIpaPollerService;
        this.freeIpaService = freeIpaService;
        this.wiamClient = wiamClient;
        this.entitlementService = entitlementService;
    }

    @Override
    public String selector() {
        return EnvStartHandlerSelectors.SYNCHRONIZE_USERS_HANDLER_EVENT.name();
    }

    @Override
    public void accept(Event<EnvironmentStartDto> environmentStartDtoEvent) {
        LOGGER.debug("User synchronization is {}.", synchronizeOnStartEnabled ? "enabled" : "disabled");
        EnvironmentDto environmentDto = environmentStartDtoEvent.getData().getEnvironmentDto();
        try {
            if (synchronizeOnStartEnabled) {
                freeIpaService.describe(environmentDto.getResourceCrn()).ifPresent(freeIpa -> {
                    if (freeIpa.getStatus() != null && freeIpa.getAvailabilityStatus() != null && !freeIpa.getAvailabilityStatus().isAvailable()) {
                        throw new FreeIpaOperationFailedException("FreeIPA is not in AVAILABLE state to synchronize users! Current state is: " +
                                freeIpa.getStatus().name());
                    }
                });
                if (entitlementService.isWiamUsersyncRoutingEnabled(environmentDto.getAccountId())) {
                    LOGGER.debug("Initiating usersync via WIAM");
                    try {
                        wiamClient.syncUsersInEnvironment(environmentDto.getAccountId(), environmentDto.getResourceCrn(), MDCBuilder.getOrGenerateRequestId());
                    } catch (Exception e) {
                        LOGGER.error("Initiating usersync via WIAM failed", e);
                    }
                } else {
                    freeIpaPollerService.waitForSynchronizeUsers(environmentDto.getId(), environmentDto.getResourceCrn());
                }
            }
            EnvStartEvent envStartEvent = EnvStartEvent.EnvStartEventBuilder.anEnvStartEvent()
                    .withSelector(EnvStartStateSelectors.FINISH_ENV_START_EVENT.selector())
                    .withResourceId(environmentDto.getId())
                    .withResourceName(environmentDto.getName())
                    .withDataHubStartAction(environmentStartDtoEvent.getData().getDataHubStart())
                    .build();
            eventSender().sendEvent(envStartEvent, environmentStartDtoEvent.getHeaders());
        } catch (Exception e) {
            EnvStartFailedEvent failedEvent = new EnvStartFailedEvent(environmentDto, e, EnvironmentStatus.START_SYNCHRONIZE_USERS_FAILED);
            eventSender().sendEvent(failedEvent, environmentStartDtoEvent.getHeaders());
        }
    }

}
