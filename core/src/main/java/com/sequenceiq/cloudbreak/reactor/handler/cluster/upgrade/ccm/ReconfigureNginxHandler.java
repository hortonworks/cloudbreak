package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.ccm;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmService;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmReconfigureNginxRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmReconfigureNginxResult;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class ReconfigureNginxHandler extends ExceptionCatcherEventHandler<UpgradeCcmReconfigureNginxRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReconfigureNginxHandler.class);

    @Inject
    private UpgradeCcmService upgradeCcmService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpgradeCcmReconfigureNginxRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpgradeCcmReconfigureNginxRequest> event) {
        LOGGER.error("Reconfiguring NGINX for CCM upgrade has failed", e);
        return new UpgradeCcmFailedEvent(resourceId, event.getData().getOldTunnel(), getClass(), e);
    }

    @Override
    public Selectable doAccept(HandlerEvent<UpgradeCcmReconfigureNginxRequest> event) {
        UpgradeCcmReconfigureNginxRequest request = event.getData();
        Long stackId = request.getResourceId();
        LOGGER.info("NGINX reconfiguration is needed for previous CCM tunnel type");
        try {
            upgradeCcmService.reconfigureNginx(stackId);
        } catch (CloudbreakOrchestratorException e) {
            LOGGER.debug("Failed reconfiguring NGINX with salt state");
            return new UpgradeCcmFailedEvent(stackId, request.getOldTunnel(), getClass(), e);
        }
        return new UpgradeCcmReconfigureNginxResult(stackId, request.getClusterId(), request.getOldTunnel());
    }
}
