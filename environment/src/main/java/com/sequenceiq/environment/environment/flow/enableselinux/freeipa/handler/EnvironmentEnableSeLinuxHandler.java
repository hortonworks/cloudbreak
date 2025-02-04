package com.sequenceiq.environment.environment.flow.enableselinux.freeipa.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.flow.enableselinux.freeipa.event.EnvironmentEnableSeLinuxEvent;
import com.sequenceiq.environment.environment.flow.enableselinux.freeipa.event.EnvironmentEnableSeLinuxFailedEvent;
import com.sequenceiq.environment.environment.flow.enableselinux.freeipa.event.EnvironmentEnableSeLinuxHandlerSelectors;
import com.sequenceiq.environment.environment.flow.enableselinux.freeipa.event.EnvironmentEnableSeLinuxStateSelectors;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaPollerService;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.environment.exception.FreeIpaOperationFailedException;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

@Component
public class EnvironmentEnableSeLinuxHandler extends EventSenderAwareHandler<EnvironmentEnableSeLinuxEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentEnableSeLinuxHandler.class);

    private final FreeIpaService freeIpaService;

    private final FreeIpaPollerService freeIpaPollerService;

    protected EnvironmentEnableSeLinuxHandler(EventSender eventSender, FreeIpaService freeIpaService, FreeIpaPollerService freePollerIpaService) {
        super(eventSender);
        this.freeIpaService = freeIpaService;
        freeIpaPollerService = freePollerIpaService;
    }

    @Override
    public String selector() {
        return EnvironmentEnableSeLinuxHandlerSelectors.ENABLE_SELINUX_FREEIPA_HANDLER.selector();
    }

    @Override
    public void accept(Event<EnvironmentEnableSeLinuxEvent> enableSeLinuxEventEvent) {
        LOGGER.debug("In EnvironmentEnableSeLinuxHandler.accept");
        EnvironmentEnableSeLinuxEvent enableSeLinuxEvent = enableSeLinuxEventEvent.getData();
        try {
            freeIpaService.describe(enableSeLinuxEvent.getResourceCrn()).ifPresentOrElse(freeIpa -> {
                if (freeIpa.getStatus() == null || freeIpa.getAvailabilityStatus() == null) {
                    throw new FreeIpaOperationFailedException("FreeIPA status is unpredictable, Enable SeLinux interrupted.");
                } else if (!freeIpa.getStatus().isAvailable()) {
                    throw new FreeIpaOperationFailedException("FreeIPA is not in a valid state to Enable SeLinux. Current state is: " +
                            freeIpa.getStatus().name());
                } else {
                    LOGGER.info("FreeIPA will be Enable SeLinux.");
                    freeIpaPollerService.waitForEnableSeLinux(
                            enableSeLinuxEvent.getResourceId(),
                            enableSeLinuxEvent.getResourceCrn());
                }

                EnvironmentEnableSeLinuxEvent result = EnvironmentEnableSeLinuxEvent.builder()
                        .withSelector(EnvironmentEnableSeLinuxStateSelectors.FINISH_ENABLE_SELINUX_FREEIPA_EVENT.selector())
                        .withResourceCrn(enableSeLinuxEvent.getResourceCrn())
                        .withResourceId(enableSeLinuxEvent.getResourceId())
                        .withResourceName(enableSeLinuxEvent.getResourceName())
                        .build();

                eventSender().sendEvent(result, enableSeLinuxEventEvent.getHeaders());
                LOGGER.debug("ENABLE_SELINUX_FREEIPA_EVENT event sent");

            }, () -> {
                throw new FreeIpaOperationFailedException(String.format("FreeIPA cannot be found for environment %s",
                        enableSeLinuxEvent.getResourceName()));
            });

        } catch (Exception e) {
            EnvironmentEnableSeLinuxFailedEvent failedEvent =
                    new EnvironmentEnableSeLinuxFailedEvent(enableSeLinuxEvent, e, EnvironmentStatus.ENABLE_SELINUX_ON_FREEIPA_FAILED);
            eventSender().sendEvent(failedEvent, enableSeLinuxEventEvent.getHeaders());
            LOGGER.debug("ENABLE_SELINUX_ON_FREEIPA_FAILED event sent");
        }
    }

}
