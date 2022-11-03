package com.sequenceiq.environment.environment.flow.upgrade.ccm.handler;

import static com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmHandlerSelectors.UPGRADE_CCM_DATALAKE_HANDLER;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmEvent;
import com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmFailedEvent;
import com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmStateSelectors;
import com.sequenceiq.environment.environment.service.sdx.SdxService;
import com.sequenceiq.environment.environment.service.sdx.SdxUpgradeCcmPollerService;
import com.sequenceiq.environment.exception.SdxOperationFailedException;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;
import com.sequenceiq.sdx.api.model.SdxCcmUpgradeResponse;

@Component
public class UpgradeCcmOnDatalakeHandler extends EventSenderAwareHandler<EnvironmentDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeCcmOnDatalakeHandler.class);

    private final SdxService sdxService;

    private final SdxUpgradeCcmPollerService pollerService;

    protected UpgradeCcmOnDatalakeHandler(EventSender eventSender, SdxService sdxService, SdxUpgradeCcmPollerService pollerService) {
        super(eventSender);
        this.sdxService = sdxService;
        this.pollerService = pollerService;
    }

    @Override
    public String selector() {
        return UPGRADE_CCM_DATALAKE_HANDLER.name();
    }

    @Override
    public void accept(Event<EnvironmentDto> environmentDtoEvent) {
        LOGGER.debug("In UpgradeCcmOnDatalakeHandler.accept");
        EnvironmentDto environmentDto = environmentDtoEvent.getData();
        try {
            LOGGER.debug("Calling Upgrade CCM endpoint on data lake service");
            SdxCcmUpgradeResponse ccmUpgradeResponse = sdxService.upgradeCcm(environmentDto.getResourceCrn());
            switch (ccmUpgradeResponse.getResponseType()) {
                case ERROR:
                    String message = String.format("Upgrade CCM returned with reason: %s", ccmUpgradeResponse.getReason());
                    LOGGER.warn(message);
                    sendFailedEvent(environmentDtoEvent, environmentDto, new SdxOperationFailedException(message));
                    return;
                case TRIGGERED:
                    LOGGER.debug("Waiting for data lake Upgrade CCM flow to finish.");
                    pollerService.waitForUpgradeCcm(environmentDto.getId(), ccmUpgradeResponse.getResourceCrn());
                    break;
                case SKIP:
                    LOGGER.debug("Upgrade CCM is skipped due to {}", ccmUpgradeResponse.getReason());
                    break;
                default:
                    message = String.format("Unknown response type: %s", ccmUpgradeResponse.getResponseType());
                    LOGGER.warn(message);
                    sendFailedEvent(environmentDtoEvent, environmentDto, new SdxOperationFailedException(message));
                    return;
            }

            UpgradeCcmEvent upgradeCcmEvent = UpgradeCcmEvent.builder()
                    .withSelector(UpgradeCcmStateSelectors.UPGRADE_CCM_DATAHUB_EVENT.selector())
                    .withResourceCrn(environmentDto.getResourceCrn())
                    .withResourceId(environmentDto.getId())
                    .withResourceName(environmentDto.getName())
                    .build();

            eventSender().sendEvent(upgradeCcmEvent, environmentDtoEvent.getHeaders());
            LOGGER.debug("UPGRADE_CCM_DATAHUB_EVENT event sent");
        } catch (Exception e) {
            sendFailedEvent(environmentDtoEvent, environmentDto, e);
        }
    }

    private void sendFailedEvent(Event<EnvironmentDto> environmentDtoEvent, EnvironmentDto environmentDto, Exception e) {
        UpgradeCcmFailedEvent failedEvent = new UpgradeCcmFailedEvent(environmentDto, e, EnvironmentStatus.UPGRADE_CCM_ON_DATALAKE_FAILED);
        eventSender().sendEvent(failedEvent, environmentDtoEvent.getHeaders());
        LOGGER.debug("UPGRADE_CCM_ON_DATALAKE_FAILED event sent");
    }
}
