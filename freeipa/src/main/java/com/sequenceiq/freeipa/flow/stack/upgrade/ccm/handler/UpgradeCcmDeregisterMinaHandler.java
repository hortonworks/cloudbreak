package com.sequenceiq.freeipa.flow.stack.upgrade.ccm.handler;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmHandlerSelector.UPGRADE_CCM_DEREGISTER_MINA_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_CLEANING_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_DEREGISTER_MINA_FINISHED_EVENT;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmService;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event.UpgradeCcmEvent;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event.UpgradeCcmFailureEvent;

@Component
public class UpgradeCcmDeregisterMinaHandler extends AbstractUpgradeCcmEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeCcmDeregisterMinaHandler.class);

    @Inject
    private UpgradeCcmService upgradeCcmService;

    @Override
    public String selector() {
        return UPGRADE_CCM_DEREGISTER_MINA_EVENT.event();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpgradeCcmEvent> event) {
        LOGGER.error("Deregistering mina for CCM upgrade has failed", e);
        return new UpgradeCcmFailureEvent(
                UPGRADE_CCM_CLEANING_FAILED_EVENT.event(),
                resourceId,
                event.getData().getOldTunnel(),
                getClass(),
                e,
                event.getData().getRevertTime(),
                "Deregistering MINA failed for CCM upgrade, ",
                ERROR
        );
    }

    @Override
    protected Selectable doAccept(HandlerEvent<UpgradeCcmEvent> event) {
        UpgradeCcmEvent request = event.getData();
        upgradeCcmService.changeTunnel(request.getResourceId(), Tunnel.latestUpgradeTarget());
        if (request.getOldTunnel().useCcmV1()) {
            LOGGER.info("Deregistering mina for CCM upgrade...");
            try {
                upgradeCcmService.deregisterMina(request.getResourceId());
            } catch (Exception e) {
                LOGGER.info("Deregistering mina is failed, but the stack is operational with CCMv2");
                UpgradeCcmEvent resultEvent = UPGRADE_CCM_DEREGISTER_MINA_FINISHED_EVENT.createBasedOn(request);
                resultEvent.setMinaRemoved(Boolean.FALSE);
                return resultEvent;
            }
        } else {
            LOGGER.info("Deregistering mina is skipped for previous tunnel type '{}'", request.getOldTunnel());
        }
        return UPGRADE_CCM_DEREGISTER_MINA_FINISHED_EVENT.createBasedOn(request);
    }

}
