package com.sequenceiq.environment.environment.flow.upgrade.ccm.handler;

import static com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmHandlerSelectors.UPGRADE_CCM_DATAHUB_HANDLER;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.api.model.CcmUpgradeResponseType;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXCcmUpgradeV1Response;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmEvent;
import com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmFailedEvent;
import com.sequenceiq.environment.environment.flow.upgrade.ccm.event.UpgradeCcmStateSelectors;
import com.sequenceiq.environment.environment.service.datahub.DatahubService;
import com.sequenceiq.environment.environment.service.datahub.DatahubUpgradeCcmPollerService;
import com.sequenceiq.environment.exception.DatahubOperationFailedException;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;

@Component
public class UpgradeCcmOnDatahubHandler extends EventSenderAwareHandler<EnvironmentDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeCcmOnDatahubHandler.class);

    private final DatahubService datahubService;

    private final DatahubUpgradeCcmPollerService upgradeCcmService;

    protected UpgradeCcmOnDatahubHandler(EventSender eventSender, DatahubService datahubService, DatahubUpgradeCcmPollerService upgradeCcmService) {
        super(eventSender);
        this.datahubService = datahubService;
        this.upgradeCcmService = upgradeCcmService;
    }

    @Override
    public String selector() {
        return UPGRADE_CCM_DATAHUB_HANDLER.name();
    }

    @Override
    public void accept(Event<EnvironmentDto> environmentDtoEvent) {
        LOGGER.debug("In UpgradeCcmOnDatahubHandler.accept");
        EnvironmentDto environmentDto = environmentDtoEvent.getData();
        try {
            List<String> dataHubCrns = getDatahubCrns(environmentDto.getResourceCrn());
            String errorReason = null;
            if (!dataHubCrns.isEmpty()) {
                LOGGER.debug("The following datahub crns will be tried for upgrade CCM: {}", dataHubCrns);
                List<DistroXCcmUpgradeV1Response> upgradeResponses = initUpgradeCcm(dataHubCrns);
                if (hasAllErroredOut(upgradeResponses)) {
                    String message = "All of the Data Hubs returned error for Upgrade CCM trigger: " + getReasons(upgradeResponses);
                    LOGGER.warn(message);
                    sendFailedEvent(environmentDtoEvent, environmentDto, new DatahubOperationFailedException(message));
                    return;
                }
                logNotTriggered(upgradeResponses);
                if (hasAnyErroredOut(upgradeResponses)) {
                    errorReason = "Some Data Hubs could not be upgraded.";
                }
                upgradeCcmService.waitForUpgradeOnFlowIds(environmentDto.getId(), getTriggeredFlows(upgradeResponses));
            } else {
                LOGGER.info("There were no Data Hubs created for environment {}", environmentDto.getResourceCrn());
            }
            UpgradeCcmEvent upgradeCcmEvent = UpgradeCcmEvent.builder()
                    .withSelector(UpgradeCcmStateSelectors.FINISH_UPGRADE_CCM_EVENT.selector())
                    .withResourceCrn(environmentDto.getResourceCrn())
                    .withResourceId(environmentDto.getId())
                    .withResourceName(environmentDto.getName())
                    .withErrorReason(errorReason)
                    .build();

            eventSender().sendEvent(upgradeCcmEvent, environmentDtoEvent.getHeaders());
            LOGGER.debug("FINISH_UPGRADE_CCM_EVENT event sent");
        } catch (Exception e) {
            sendFailedEvent(environmentDtoEvent, environmentDto, e);
        }
    }

    private List<String> getDatahubCrns(String environmentCrn) {
        StackViewV4Responses dataHubs = datahubService.list(environmentCrn);
        List<String> dataHubCrns = dataHubs.getResponses().stream()
                .map(StackViewV4Response::getCrn)
                .collect(Collectors.toList());
        return dataHubCrns;
    }

    private boolean hasAllErroredOut(List<DistroXCcmUpgradeV1Response> upgradeResponses) {
        return upgradeResponses.stream().allMatch(r -> r.getResponseType() == CcmUpgradeResponseType.ERROR);
    }

    private boolean hasAnyErroredOut(List<DistroXCcmUpgradeV1Response> upgradeResponses) {
        return upgradeResponses.stream().anyMatch(r -> r.getResponseType() == CcmUpgradeResponseType.ERROR);
    }

    private String getReasons(List<DistroXCcmUpgradeV1Response> upgradeResponses) {
        return upgradeResponses.stream()
                .map(r -> String.format("[Resource CRN: %s, reason: %s]", r.getResourceCrn(), r.getReason()))
                .collect(Collectors.joining("; "));
    }

    private List<DistroXCcmUpgradeV1Response> initUpgradeCcm(List<String> dataHubCrns) {
        return dataHubCrns.stream()
                .map(datahubService::upgradeCcm)
                .collect(Collectors.toList());
    }

    private void logNotTriggered(List<DistroXCcmUpgradeV1Response> responses) {
        responses.forEach(r -> {
            if (r.getResponseType() == CcmUpgradeResponseType.SKIP) {
                LOGGER.info("Datahub with CRN {} is already upgraded to the latest CCM.", r.getResourceCrn());
            }
            if (r.getResponseType() == CcmUpgradeResponseType.ERROR) {
                LOGGER.warn("Datahub with CRN {} failed to upgrade CCM due to {}.", r.getResourceCrn(), r.getReason());
            }
        });
    }

    private List<FlowIdentifier> getTriggeredFlows(List<DistroXCcmUpgradeV1Response> upgradeResponses) {
        return upgradeResponses.stream()
                .filter(r -> r.getResponseType() == CcmUpgradeResponseType.TRIGGERED)
                .map(DistroXCcmUpgradeV1Response::getFlowIdentifier)
                .collect(Collectors.toList());
    }

    private void sendFailedEvent(Event<EnvironmentDto> environmentDtoEvent, EnvironmentDto environmentDto, Exception e) {
        UpgradeCcmFailedEvent failedEvent = new UpgradeCcmFailedEvent(environmentDto, e, EnvironmentStatus.UPGRADE_CCM_ON_DATAHUB_FAILED);
        eventSender().sendEvent(failedEvent, environmentDtoEvent.getHeaders());
        LOGGER.debug("UPGRADE_CCM_ON_DATAHUB_FAILED event sent");
    }
}
