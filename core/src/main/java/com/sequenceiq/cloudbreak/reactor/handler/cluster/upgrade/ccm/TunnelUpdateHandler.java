package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.ccm;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmService;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmTunnelUpdateRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmTunnelUpdateResult;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class TunnelUpdateHandler extends ExceptionCatcherEventHandler<UpgradeCcmTunnelUpdateRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TunnelUpdateHandler.class);

    @Inject
    private UpgradeCcmService upgradeCcmService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpgradeCcmTunnelUpdateRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpgradeCcmTunnelUpdateRequest> event) {
        LOGGER.error("Changing tunnel for CCM upgrade has failed", e);
        return new UpgradeCcmFailedEvent(resourceId, event.getData().getClusterId(), event.getData().getOldTunnel(), getClass(),
                e, event.getData().getRevertTime());
    }

    @Override
    public Selectable doAccept(HandlerEvent<UpgradeCcmTunnelUpdateRequest> event) {
        UpgradeCcmTunnelUpdateRequest request = event.getData();
        Long stackId = request.getResourceId();
        LOGGER.info("Changing tunnel for CCM upgrade...");
        upgradeCcmService.updateTunnel(stackId, Tunnel.latestUpgradeTarget());
        return new UpgradeCcmTunnelUpdateResult(stackId, request.getClusterId(), request.getOldTunnel(), request.getRevertTime());
    }
}
