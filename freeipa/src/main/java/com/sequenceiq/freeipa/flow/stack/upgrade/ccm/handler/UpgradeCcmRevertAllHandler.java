package com.sequenceiq.freeipa.flow.stack.upgrade.ccm.handler;

import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmHandlerSelector.UPGRADE_CCM_REVERT_ALL_FAILURE_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_FAILED_EVENT;

import java.time.Duration;
import java.time.LocalDateTime;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmService;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event.UpgradeCcmFailureEvent;

@Component
public class UpgradeCcmRevertAllHandler extends ExceptionCatcherEventHandler<UpgradeCcmFailureEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeCcmRevertAllHandler.class);

    private static final int MILLI = 1000;

    @Inject
    private UpgradeCcmService upgradeCcmService;

    @Override
    public String selector() {
        return UPGRADE_CCM_REVERT_ALL_FAILURE_EVENT.event();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpgradeCcmFailureEvent> event) {
        LOGGER.error("Reverting back everything has been failed", e);
        UpgradeCcmFailureEvent resultEvent = UPGRADE_CCM_FAILED_EVENT.createBasedOn(event.getData());
        resultEvent.setStatusReason(event.getData().getStatusReason() + " Reverting back has been failed");

        return resultEvent;
    }

    @Override
    protected Selectable doAccept(HandlerEvent<UpgradeCcmFailureEvent> event) {
        UpgradeCcmFailureEvent request = event.getData();
        String errText = "";

        try {
            LOGGER.info("CCM upgrade reverting");
            upgradeCcmService.changeTunnel(request.getResourceId(), request.getOldTunnel());
            long waitTimeInMilisec = 0;
            if (request.getRevertTime() != null) {
                waitTimeInMilisec = Duration.between(LocalDateTime.now(), event.getData().getRevertTime()).getSeconds() * MILLI;
            } else {
                LOGGER.warn("Revert time is null!");
            }
            LOGGER.info("CCM upgrade milliseconds back to reverting {}ms", waitTimeInMilisec);
            if (waitTimeInMilisec > 0) {
                Thread.sleep(waitTimeInMilisec);
            }
            upgradeCcmService.registerClusterProxyAndCheckHealth(request.getResourceId());
            upgradeCcmService.pushSaltStates(request.getResourceId());
        } catch (Exception e) {
            LOGGER.error("Exception happened when reverting all ccm upgrade. Flow finished ", e);
            errText = " Reverting back has been failed";
        }

        UpgradeCcmFailureEvent resultEvent = UPGRADE_CCM_FAILED_EVENT.createBasedOn(request);
        resultEvent.setStatusReason(event.getData().getStatusReason() + errText);

        return resultEvent;
    }

}
