package com.sequenceiq.freeipa.flow.stack.upgrade.ccm.handler;

import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmHandlerSelector.UPGRADE_CCM_REVERT_FAILURE_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_FAILED_EVENT;

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
public class UpgradeCcmRevertHandler extends ExceptionCatcherEventHandler<UpgradeCcmFailureEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeCcmRevertHandler.class);

    @Inject
    private UpgradeCcmService upgradeCcmService;

    @Override
    public String selector() {
        return UPGRADE_CCM_REVERT_FAILURE_EVENT.event();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpgradeCcmFailureEvent> event) {
        LOGGER.error("Reverting back has been failed", e);
        UpgradeCcmFailureEvent resultEvent = UPGRADE_CCM_FAILED_EVENT.createBasedOn(event.getData());
        resultEvent.setStatusReason(event.getData().getStatusReason() + " Reverting back has been failed");

        return resultEvent;
    }

    @Override
    protected Selectable doAccept(HandlerEvent<UpgradeCcmFailureEvent> event) {
        UpgradeCcmFailureEvent request = event.getData();
        String errText = "";
        LOGGER.info("CCM upgrade reverting");
        try {
            upgradeCcmService.changeTunnel(request.getResourceId(), request.getOldTunnel());
            upgradeCcmService.pushSaltStates(request.getResourceId());
        } catch (Exception e) {
            LOGGER.error("Exception happened when reverting ccm upgrade. Flow finished ", e);
            errText = " Reverting back has been failed";
        }
        UpgradeCcmFailureEvent resultEvent = UPGRADE_CCM_FAILED_EVENT.createBasedOn(request);
        resultEvent.setStatusReason(request.getStatusReason() + errText);

        return resultEvent;
    }

}
