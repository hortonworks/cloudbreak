package com.sequenceiq.freeipa.flow.stack.upgrade.ccm.handler;

import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmHandlerSelector.UPGRADE_CCM_APPLY_UPGRADE_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_UPGRADE_FINISHED_EVENT;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmService;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event.UpgradeCcmEvent;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event.UpgradeCcmFailureEvent;

import reactor.bus.Event;

@Component
public class UpgradeCcmUpgradeHandler extends AbstractUpgradeCcmEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeCcmUpgradeHandler.class);

    @Inject
    private UpgradeCcmService upgradeCcmService;

    @Override
    public String selector() {
        return UPGRADE_CCM_APPLY_UPGRADE_EVENT.event();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpgradeCcmEvent> event) {
        LOGGER.error("Running upgrade for CCM upgrade has failed", e);
        return new UpgradeCcmFailureEvent(UPGRADE_CCM_FAILED_EVENT.event(), resourceId, event.getData().getOldTunnel(), getClass(), e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<UpgradeCcmEvent> event) {
        UpgradeCcmEvent request = event.getData();
        if (request.getOldTunnel().useCcmV1()) {
            try {
                LOGGER.info("Running upgrade state for CCM...");
                upgradeCcmService.upgrade(request.getResourceId());
            } catch (CloudbreakOrchestratorException e) {
                LOGGER.debug("Failed applying CCM upgrade state");
                return new UpgradeCcmFailureEvent(UPGRADE_CCM_FAILED_EVENT.event(), request.getResourceId(),
                        event.getData().getOldTunnel(), getClass(), e);
            }
        } else {
            LOGGER.info("Running upgrade step is skipped for previous tunnel type '{}'", request.getOldTunnel());
        }
        return UPGRADE_CCM_UPGRADE_FINISHED_EVENT.createBasedOn(request);
    }

}
