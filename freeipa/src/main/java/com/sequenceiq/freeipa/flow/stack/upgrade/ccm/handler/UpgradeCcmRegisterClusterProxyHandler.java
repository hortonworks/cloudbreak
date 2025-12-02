package com.sequenceiq.freeipa.flow.stack.upgrade.ccm.handler;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmHandlerSelector.UPGRADE_CCM_REGISTER_CLUSTER_PROXY_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmHandlerSelector.UPGRADE_CCM_REVERT_ALL_FAILURE_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_REGISTER_CLUSTER_PROXY_FINISHED_EVENT;

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
public class UpgradeCcmRegisterClusterProxyHandler extends AbstractUpgradeCcmEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeCcmRegisterClusterProxyHandler.class);

    @Inject
    private UpgradeCcmService upgradeCcmService;

    @Override
    public String selector() {
        return UPGRADE_CCM_REGISTER_CLUSTER_PROXY_EVENT.event();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpgradeCcmEvent> event) {
        LOGGER.error("Registering cluster proxy for CCM upgrade has failed", e);
        return new UpgradeCcmFailureEvent(
                UPGRADE_CCM_REVERT_ALL_FAILURE_EVENT.event(),
                resourceId,
                event.getData().getOldTunnel(),
                getClass(),
                e,
                event.getData().getRevertTime(),
                "Upgrading CCM is failed, registering cluster proxy has been failed.",
                ERROR
        );
    }

    @Override
    protected Selectable doAccept(HandlerEvent<UpgradeCcmEvent> event) {
        UpgradeCcmEvent request = event.getData();
        upgradeCcmService.changeTunnel(request.getResourceId(), Tunnel.latestUpgradeTarget());
        if (request.getOldTunnel().useCcmV1()) {
            LOGGER.info("Registering to cluster proxy for CCM upgrade...");
            upgradeCcmService.registerClusterProxyAndCheckHealth(request.getResourceId());
        } else {
            LOGGER.info("Registering to cluster proxy step is skipped for previous tunnel type '{}'", request.getOldTunnel());
        }
        return UPGRADE_CCM_REGISTER_CLUSTER_PROXY_FINISHED_EVENT.createBasedOn(request);
    }

}
