package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.ccm;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmEvent.UPGRADE_CCM_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmEvent.UPGRADE_CCM_REVERT_SALTSTATE_COMMENCE_EVENT;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmService;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmFailedEvent;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class RevertSaltStatesHandler extends ExceptionCatcherEventHandler<UpgradeCcmFailedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RevertSaltStatesHandler.class);

    @Inject
    private UpgradeCcmService upgradeCcmService;

    @Override
    public String selector() {
        return UPGRADE_CCM_REVERT_SALTSTATE_COMMENCE_EVENT.event();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpgradeCcmFailedEvent> event) {
        LOGGER.error("Reverting salt state for CCM upgrade has failed", e);
        return new UpgradeCcmFailedEvent(UPGRADE_CCM_FAILED_EVENT.event(), resourceId, event.getData().getClusterId(),
                event.getData().getOldTunnel(), getClass(), e, event.getData().getRevertTime());
    }

    @Override
    public Selectable doAccept(HandlerEvent<UpgradeCcmFailedEvent> event) {
        UpgradeCcmFailedEvent request = event.getData();
        Long stackId = request.getResourceId();
        LOGGER.info("Reverting salt states is starting");
        upgradeCcmService.pushSaltState(stackId, event.getData().getClusterId());
        LOGGER.info("Upgrade CCM revert salt states finished");
        return new UpgradeCcmFailedEvent(UPGRADE_CCM_FAILED_EVENT.event(), stackId, request.getClusterId(), request.getOldTunnel(), getClass(),
                request.getException(), request.getRevertTime());
    }
}
