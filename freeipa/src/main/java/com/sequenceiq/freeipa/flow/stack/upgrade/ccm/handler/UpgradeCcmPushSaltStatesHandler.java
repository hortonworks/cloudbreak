package com.sequenceiq.freeipa.flow.stack.upgrade.ccm.handler;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmHandlerSelector.UPGRADE_CCM_PUSH_SALT_STATES_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_PUSH_SALT_STATES_FINISHED_EVENT;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmService;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event.UpgradeCcmEvent;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event.UpgradeCcmFailureEvent;

@Component
public class UpgradeCcmPushSaltStatesHandler extends AbstractUpgradeCcmEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeCcmPushSaltStatesHandler.class);

    @Inject
    private UpgradeCcmService upgradeCcmService;

    @Override
    public String selector() {
        return UPGRADE_CCM_PUSH_SALT_STATES_EVENT.event();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpgradeCcmEvent> event) {
        LOGGER.error("Pushing salt states for CCM upgrade has failed", e);
        return new UpgradeCcmFailureEvent(
                UPGRADE_CCM_FAILED_EVENT.event(),
                resourceId,
                event.getData().getOldTunnel(),
                getClass(),
                e,
                event.getData().getRevertTime(),
                "Upgrading CCM is failed, pushing salt states has been failed.",
                ERROR
        );
    }

    @Override
    protected Selectable doAccept(HandlerEvent<UpgradeCcmEvent> event) {
        UpgradeCcmEvent request = event.getData();
        upgradeCcmService.changeTunnel(request.getResourceId(), Tunnel.latestUpgradeTarget());
        if (request.getOldTunnel().useCcmV1()) {
            LOGGER.info("Pushing salt states for CCM upgrade...");
            try {
                upgradeCcmService.pushSaltStates(request.getResourceId());
            } catch (CloudbreakOrchestratorException e) {
                LOGGER.debug("Failed pushing salt states");
                return new UpgradeCcmFailureEvent(
                        UPGRADE_CCM_FAILED_EVENT.event(),
                        request.getResourceId(),
                        event.getData().getOldTunnel(),
                        getClass(),
                        e,
                        event.getData().getRevertTime(),
                        "Upgrading CCM is failed, pushing salt states has been failed.",
                        ERROR
                );
            }
        } else {
            LOGGER.info("Pushing salt states step is skipped for previous tunnel type '{}'", request.getOldTunnel());
        }
        return UPGRADE_CCM_PUSH_SALT_STATES_FINISHED_EVENT.createBasedOn(request);
    }

}
