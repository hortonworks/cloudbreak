package com.sequenceiq.freeipa.flow.stack.upgrade.ccm.handler;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmHandlerSelector.UPGRADE_CCM_APPLY_UPGRADE_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_FAILED_REVERT_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_UPGRADE_FINISHED_EVENT;

import java.time.LocalDateTime;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmService;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event.UpgradeCcmEvent;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event.UpgradeCcmFailureEvent;
import com.sequenceiq.freeipa.service.upgrade.ccm.CcmParametersConfigService;

@Component
public class UpgradeCcmUpgradeHandler extends AbstractUpgradeCcmEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeCcmUpgradeHandler.class);

    @Inject
    private UpgradeCcmService upgradeCcmService;

    @Inject
    private CcmParametersConfigService ccmParametersConfigService;

    @Override
    public String selector() {
        return UPGRADE_CCM_APPLY_UPGRADE_EVENT.event();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpgradeCcmEvent> event) {
        LOGGER.error("Running upgrade for CCM upgrade has failed", e);
        LocalDateTime revertDate = getRevertDate();
        return new UpgradeCcmFailureEvent(
                UPGRADE_CCM_FAILED_REVERT_EVENT.event(),
                resourceId,
                event.getData().getOldTunnel(),
                getClass(),
                e,
                revertDate,
                "Upgrading CCM is failed.",
                ERROR
        );
    }

    @Override
    protected Selectable doAccept(HandlerEvent<UpgradeCcmEvent> event) {
        UpgradeCcmEvent request = event.getData();
        upgradeCcmService.changeTunnel(request.getResourceId(), Tunnel.latestUpgradeTarget());
        if (request.getOldTunnel().useCcmV1()) {
            return tryToUpgrade(event, request);
        } else {
            LOGGER.info("Running upgrade step is skipped for previous tunnel type '{}'", request.getOldTunnel());
            return UPGRADE_CCM_UPGRADE_FINISHED_EVENT.createBasedOn(request);
        }
    }

    private StackEvent tryToUpgrade(HandlerEvent<UpgradeCcmEvent> event, UpgradeCcmEvent request) {
        try {
            LOGGER.info("Running upgrade state for CCM...");
            upgradeCcmService.upgrade(request.getResourceId());
            UpgradeCcmEvent resultEvent = UPGRADE_CCM_UPGRADE_FINISHED_EVENT.createBasedOn(request);
            resultEvent.setUpgradeDateTime(getRevertDate());
            return resultEvent;
        } catch (CloudbreakOrchestratorException e) {
            LOGGER.debug("Failed applying CCM upgrade state", e);
            return new UpgradeCcmFailureEvent(
                    UPGRADE_CCM_FAILED_REVERT_EVENT.event(),
                    request.getResourceId(),
                    event.getData().getOldTunnel(),
                    getClass(),
                    e,
                    event.getData().getRevertTime(),
                    "Upgrading CCM is failed.",
                    ERROR
            );
        }
    }

    private LocalDateTime getRevertDate() {
        return LocalDateTime.now().plusMinutes(ccmParametersConfigService.getActivationInMinutes());
    }

}
