package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.ccm;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmEvent.UPGRADE_CCM_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmEvent.UPGRADE_CCM_REVERT_ALL_COMMENCE_EVENT;

import java.time.Duration;
import java.time.LocalDateTime;

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
public class RevertAllHandler extends ExceptionCatcherEventHandler<UpgradeCcmFailedEvent> {
    private static final int MILLI = 1000;

    private static final Logger LOGGER = LoggerFactory.getLogger(RevertAllHandler.class);

    @Inject
    private UpgradeCcmService upgradeCcmService;

    @Override
    public String selector() {
        return UPGRADE_CCM_REVERT_ALL_COMMENCE_EVENT.event();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpgradeCcmFailedEvent> event) {
        LOGGER.error("Reverting all for CCM upgrade has failed", e);
        return new UpgradeCcmFailedEvent(UPGRADE_CCM_FAILED_EVENT.event(), resourceId, event.getData().getClusterId(), event.getData().getOldTunnel(),
                getClass(), e, event.getData().getRevertTime());
    }

    @Override
    public Selectable doAccept(HandlerEvent<UpgradeCcmFailedEvent> event) {
        UpgradeCcmFailedEvent request = event.getData();
        Long stackId = request.getResourceId();
        Long clusterId = request.getClusterId();
        LOGGER.info("Reverting all is starting");
        upgradeCcmService.registerClusterProxy(stackId);
        long ms = 0;
        if (event.getData().getRevertTime() != null) {
            ms = Duration.between(LocalDateTime.now(), event.getData().getRevertTime()).getSeconds() * MILLI;
        } else {
            LOGGER.warn("Revert time is not set!");
        }
        LOGGER.info("CCM upgrade milliseconds till reverting {}ms", ms);
        if (ms > 0) {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException e) {
                LOGGER.warn("Waiting for revert was interrupted", e);
            }
        }
        upgradeCcmService.healthCheck(stackId);
        upgradeCcmService.pushSaltState(stackId, clusterId);
        LOGGER.info("Upgrade CCM revert all finished");
        return new UpgradeCcmFailedEvent(UPGRADE_CCM_FAILED_EVENT.event(), stackId, request.getClusterId(), request.getOldTunnel(), getClass(),
                request.getException(), request.getRevertTime());
    }
}
