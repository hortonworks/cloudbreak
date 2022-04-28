package com.sequenceiq.environment.environment.flow.upgrade.ccm.handler;

import static com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmHandlerSelectors.UPGRADE_CCM_FREEIPA_HANDLER;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmEvent;
import com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmFailedEvent;
import com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmStateSelectors;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaPollerService;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.environment.exception.FreeIpaOperationFailedException;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;

@Component
public class UpgradeCcmOnFreeIpaHandler extends EventSenderAwareHandler<EnvironmentDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeCcmOnFreeIpaHandler.class);

    private final FreeIpaService freeIpaService;

    private final FreeIpaPollerService freeIpaPollerService;

    protected UpgradeCcmOnFreeIpaHandler(EventSender eventSender, FreeIpaService freeIpaService, FreeIpaPollerService freePollerIpaService) {
        super(eventSender);
        this.freeIpaService = freeIpaService;
        this.freeIpaPollerService = freePollerIpaService;
    }

    @Override
    public String selector() {
        return UPGRADE_CCM_FREEIPA_HANDLER.name();
    }

    @Override
    public void accept(Event<EnvironmentDto> environmentDtoEvent) {
        LOGGER.debug("In UpgradeCcmOnFreeIpaHandler.accept");
        EnvironmentDto environmentDto = environmentDtoEvent.getData();
        try {
            freeIpaService.describe(environmentDto.getResourceCrn()).ifPresentOrElse(freeIpa -> {
                if (freeIpa.getStatus() == null || freeIpa.getAvailabilityStatus() == null) {
                    throw new FreeIpaOperationFailedException("FreeIPA status is unpredictable, upgrading of Cluster Connectivity Manager will be interrupted.");
                } else if (!freeIpa.getStatus().isCcmUpgradeablePhase()) {
                    throw new FreeIpaOperationFailedException("FreeIPA is not in a valid state to upgrade Cluster Connectivity Manager. Current state is: " +
                            freeIpa.getStatus().name());
                } else {
                    LOGGER.info("CCM on FreeIPA will be upgraded.");
                    freeIpaPollerService.waitForCcmUpgrade(environmentDto.getId(), environmentDto.getResourceCrn());
                }

                UpgradeCcmEvent upgradeCcmEvent = UpgradeCcmEvent.builder()
                        .withSelector(UpgradeCcmStateSelectors.UPGRADE_CCM_TUNNEL_UPDATE_EVENT.selector())
                        .withResourceCrn(environmentDto.getResourceCrn())
                        .withResourceId(environmentDto.getId())
                        .withResourceName(environmentDto.getName())
                        .build();

                eventSender().sendEvent(upgradeCcmEvent, environmentDtoEvent.getHeaders());
                LOGGER.debug("UPGRADE_CCM_TUNNEL_UPDATE_EVENT event sent");

            }, () -> {
                throw new FreeIpaOperationFailedException(String.format("FreeIPA cannot be found for environment %s", environmentDto.getName()));
            });

        } catch (Exception e) {
            UpgradeCcmFailedEvent failedEvent = new UpgradeCcmFailedEvent(environmentDto, e, EnvironmentStatus.UPGRADE_CCM_ON_FREEIPA_FAILED);
            eventSender().sendEvent(failedEvent, environmentDtoEvent.getHeaders());
            LOGGER.debug("UPGRADE_CCM_ON_FREEIPA_FAILED event sent");
        }
    }
}
