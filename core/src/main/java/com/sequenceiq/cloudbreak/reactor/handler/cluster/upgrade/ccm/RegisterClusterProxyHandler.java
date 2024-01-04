package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.ccm;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmEvent.UPGRADE_CCM_REVERT_ALL_EVENT;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmService;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmRegisterClusterProxyRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmRegisterClusterProxyResult;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class RegisterClusterProxyHandler extends ExceptionCatcherEventHandler<UpgradeCcmRegisterClusterProxyRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegisterClusterProxyHandler.class);

    @Inject
    private UpgradeCcmService upgradeCcmService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpgradeCcmRegisterClusterProxyRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpgradeCcmRegisterClusterProxyRequest> event) {
        LOGGER.error("Registering cluster proxy for CCM upgrade has failed", e);
        return new UpgradeCcmFailedEvent(UPGRADE_CCM_REVERT_ALL_EVENT.event(), resourceId, event.getData().getClusterId(),
                event.getData().getOldTunnel(), getClass(), e, event.getData().getRevertTime());
    }

    @Override
    protected Selectable doAccept(HandlerEvent<UpgradeCcmRegisterClusterProxyRequest> event) {
        UpgradeCcmRegisterClusterProxyRequest request = event.getData();
        Long stackId = request.getResourceId();
        LOGGER.info("Registering to cluster proxy for CCM upgrade...");
        upgradeCcmService.updateTunnel(stackId, Tunnel.latestUpgradeTarget());
        upgradeCcmService.registerClusterProxyAndCheckHealth(stackId);
        return new UpgradeCcmRegisterClusterProxyResult(stackId, request.getClusterId(), request.getOldTunnel(), request.getRevertTime());
    }
}
