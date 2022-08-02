package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.ccm;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmService;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmDeregisterAgentRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmDeregisterAgentResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmFailedEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class DeregisterAgentHandler extends ExceptionCatcherEventHandler<UpgradeCcmDeregisterAgentRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeregisterAgentHandler.class);

    @Inject
    private UpgradeCcmService upgradeCcmService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpgradeCcmDeregisterAgentRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpgradeCcmDeregisterAgentRequest> event) {
        LOGGER.error("Deregistering agent for CCM upgrade has failed", e);
        return new UpgradeCcmFailedEvent(resourceId, event.getData().getOldTunnel(), getClass(), e);
    }

    @Override
    public Selectable doAccept(HandlerEvent<UpgradeCcmDeregisterAgentRequest> event) {
        UpgradeCcmDeregisterAgentRequest request = event.getData();
        Long stackId = request.getResourceId();
        LOGGER.info("Deregistering agent for CCM upgrade...");
        upgradeCcmService.updateTunnel(stackId);
        upgradeCcmService.deregisterAgent(stackId, request.getOldTunnel());
        return new UpgradeCcmDeregisterAgentResult(stackId, request.getClusterId(), request.getOldTunnel());
    }
}
