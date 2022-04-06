package com.sequenceiq.freeipa.flow.stack.upgrade.ccm.handler;

import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmHandlerSelector.UPGRADE_CCM_REMOVE_MINA_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_REMOVE_MINA_FINISHED_EVENT;

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
public class UpgradeCcmRemoveMinaHandler extends AbstractUpgradeCcmEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeCcmRemoveMinaHandler.class);

    @Inject
    private UpgradeCcmService upgradeCcmService;

    @Override
    public String selector() {
        return UPGRADE_CCM_REMOVE_MINA_EVENT.event();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpgradeCcmEvent> event) {
        LOGGER.error("Removing Mina for CCM upgrade has failed", e);
        return new UpgradeCcmFailureEvent(UPGRADE_CCM_FAILED_EVENT.event(), resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<UpgradeCcmEvent> event) {
        UpgradeCcmEvent request = event.getData();
        if (request.getOldTunnel().useCcmV1()) {
            LOGGER.info("Remove Mina for CCM upgrade...");
            try {
                upgradeCcmService.removeMina(request.getResourceId());
            } catch (CloudbreakOrchestratorException e) {
                LOGGER.debug("Failed removing Mina service with a salt state");
                return new UpgradeCcmFailureEvent(UPGRADE_CCM_FAILED_EVENT.event(), request.getResourceId(), e);
            }
        } else {
            LOGGER.info("Remove Mina step is skipped for previous tunnel type '{}'", request.getOldTunnel());
        }
        return UPGRADE_CCM_REMOVE_MINA_FINISHED_EVENT.createBasedOn(request);
    }

}
