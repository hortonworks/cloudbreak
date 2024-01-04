package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.ccm;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmService;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmRemoveAgentRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmRemoveAgentResult;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

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
        upgradeCcmService.removeAgentFailed(resourceId);
        return new UpgradeCcmRemoveAgentResult(resourceId, event.getData().getClusterId(), event.getData().getOldTunnel(),
                event.getData().getRevertTime(), Boolean.FALSE);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<UpgradeCcmRemoveAgentRequest> event) {
        UpgradeCcmRemoveAgentRequest request = event.getData();
        Long stackId = request.getResourceId();
        Boolean agentDeletionSucceed = Boolean.TRUE;
        LOGGER.info("Remove agent for CCM upgrade...");
        upgradeCcmService.updateTunnel(stackId, Tunnel.latestUpgradeTarget());
        try {
            upgradeCcmService.removeAgent(stackId, request.getOldTunnel());
        } catch (CloudbreakOrchestratorException e) {
            LOGGER.error("Failed removing agent with a salt state", e);
            agentDeletionSucceed = Boolean.FALSE;
            upgradeCcmService.removeAgentFailed(stackId);
        }
        return new UpgradeCcmRemoveAgentResult(stackId, request.getClusterId(), request.getOldTunnel(), request.getRevertTime(), agentDeletionSucceed);
    }
}
