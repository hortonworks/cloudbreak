package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.ccm;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmService;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmRemoveAgentRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmRemoveAgentResult;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class RemoveAgentHandler extends ExceptionCatcherEventHandler<UpgradeCcmRemoveAgentRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoveAgentHandler.class);

    @Inject
    private UpgradeCcmService upgradeCcmService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpgradeCcmRemoveAgentRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpgradeCcmRemoveAgentRequest> event) {
        LOGGER.error("Removing agent for CCM upgrade has failed", e);
        return new UpgradeCcmFailedEvent(resourceId, event.getData().getOldTunnel(), getClass(), e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<UpgradeCcmRemoveAgentRequest> event) {
        UpgradeCcmRemoveAgentRequest request = event.getData();
        Long stackId = request.getResourceId();
        LOGGER.info("Remove agent for CCM upgrade...");
        try {
            upgradeCcmService.removeAgent(stackId, request.getOldTunnel());
        } catch (CloudbreakOrchestratorException e) {
            LOGGER.debug("Failed removing agent with a salt state");
            return new UpgradeCcmFailedEvent(stackId, request.getOldTunnel(), getClass(), e);
        }
        return new UpgradeCcmRemoveAgentResult(stackId, request.getClusterId(), request.getOldTunnel());
    }
}
