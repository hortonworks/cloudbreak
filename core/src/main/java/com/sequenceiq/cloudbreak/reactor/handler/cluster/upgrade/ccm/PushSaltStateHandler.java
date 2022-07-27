package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.ccm;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmService;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmPushSaltStatesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmPushSaltStatesResult;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class PushSaltStateHandler extends ExceptionCatcherEventHandler<UpgradeCcmPushSaltStatesRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PushSaltStateHandler.class);

    @Inject
    private UpgradeCcmService upgradeCcmService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpgradeCcmPushSaltStatesRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpgradeCcmPushSaltStatesRequest> event) {
        LOGGER.error("Pushing salt states for CCM upgrade has failed", e);
        return new UpgradeCcmFailedEvent(resourceId, event.getData().getOldTunnel(), getClass(), e);
    }

    @Override
    public Selectable doAccept(HandlerEvent<UpgradeCcmPushSaltStatesRequest> event) {
        UpgradeCcmPushSaltStatesRequest request = event.getData();
        Long stackId = request.getResourceId();
        Long clusterId = request.getClusterId();
        LOGGER.info("Pushing salt states for CCM upgrade...");
        upgradeCcmService.updateTunnel(stackId);
        upgradeCcmService.pushSaltState(stackId, clusterId);
        return new UpgradeCcmPushSaltStatesResult(stackId, clusterId, request.getOldTunnel());
    }
}
