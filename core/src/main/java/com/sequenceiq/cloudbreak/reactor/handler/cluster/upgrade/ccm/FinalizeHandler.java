package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.ccm;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmService;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmFinalizeRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmFinalizeResult;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class FinalizeHandler extends ExceptionCatcherEventHandler<UpgradeCcmFinalizeRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FinalizeHandler.class);

    @Inject
    private UpgradeCcmService upgradeCcmService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpgradeCcmFinalizeRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpgradeCcmFinalizeRequest> event) {
        LOGGER.error("Finalizing CCM upgrade has failed", e);
        return new UpgradeCcmFailedEvent(resourceId, event.getData().getClusterId(), event.getData().getOldTunnel(), getClass(), e,
                event.getData().getRevertTime());
    }

    @Override
    protected Selectable doAccept(HandlerEvent<UpgradeCcmFinalizeRequest> event) {
        UpgradeCcmFinalizeRequest request = event.getData();
        Long stackId = request.getResourceId();
        LOGGER.info("Finalize CCM upgrade...");
        try {
            upgradeCcmService.finalizeUpgrade(stackId);
        } catch (CloudbreakOrchestratorException e) {
            LOGGER.error("Failed finalizing CCM upgrade with a salt state", e);
            upgradeCcmService.removeAgentFailed(stackId);
            return new UpgradeCcmFailedEvent(stackId, event.getData().getClusterId(), event.getData().getOldTunnel(), getClass(), e,
                    event.getData().getRevertTime());
        }
        return new UpgradeCcmFinalizeResult(stackId, request.getClusterId(), request.getOldTunnel(), request.getRevertTime(),
                request.getAgentDeletionSucceed());
    }
}
