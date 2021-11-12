package com.sequenceiq.environment.environment.flow.upgrade.ccm.handler;

import static com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmHandlerSelectors.UPGRADE_CCM_VALIDATION_HANDLER;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmEvent;
import com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmFailedEvent;
import com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmStateSelectors;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;

@Component
public class ValidateUpgradeCcmHandler extends EventSenderAwareHandler<EnvironmentDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidateUpgradeCcmHandler.class);

    protected ValidateUpgradeCcmHandler(EventSender eventSender) {
        super(eventSender);
    }

    @Override
    public String selector() {
        return UPGRADE_CCM_VALIDATION_HANDLER.name();
    }

    @Override
    public void accept(Event<EnvironmentDto> environmentDtoEvent) {
        LOGGER.debug("In ValidateUpgradeCcmHandler.accept");
        EnvironmentDto environmentDto = environmentDtoEvent.getData();
        try {
            // TODO action here

            UpgradeCcmEvent upgradeCcmEvent = UpgradeCcmEvent.builder()
                    .withSelector(UpgradeCcmStateSelectors.UPGRADE_CCM_FREEIPA_EVENT.selector())
                    .withResourceCrn(environmentDto.getResourceCrn())
                    .withResourceId(environmentDto.getId())
                    .withResourceName(environmentDto.getName())
                    .build();

            eventSender().sendEvent(upgradeCcmEvent, environmentDtoEvent.getHeaders());
            LOGGER.debug("UPGRADE_CCM_FREEIPA_EVENT event sent");
        } catch (Exception e) {
            UpgradeCcmFailedEvent failedEvent = new UpgradeCcmFailedEvent(environmentDto, e, EnvironmentStatus.UPGRADE_CCM_VALIDATION_FAILED);
            eventSender().sendEvent(failedEvent, environmentDtoEvent.getHeaders());
            LOGGER.debug("UPGRADE_CCM_VALIDATION_FAILED event sent");
        }
    }
}
