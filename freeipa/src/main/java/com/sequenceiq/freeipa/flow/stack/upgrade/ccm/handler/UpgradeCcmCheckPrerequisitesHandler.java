package com.sequenceiq.freeipa.flow.stack.upgrade.ccm.handler;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmHandlerSelector.UPGRADE_CCM_CHECK_PREREQUISITES_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_CHECK_PREREQUISITES_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_FAILED_EVENT;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmService;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event.UpgradeCcmEvent;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event.UpgradeCcmFailureEvent;

@Component
public class UpgradeCcmCheckPrerequisitesHandler extends AbstractUpgradeCcmEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeCcmCheckPrerequisitesHandler.class);

    @Inject
    private UpgradeCcmService upgradeCcmService;

    @Override
    public String selector() {
        return UPGRADE_CCM_CHECK_PREREQUISITES_EVENT.event();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpgradeCcmEvent> event) {
        LOGGER.error("Checking prerequisites for CCM upgrade has failed", e);
        return new UpgradeCcmFailureEvent(
                UPGRADE_CCM_FAILED_EVENT.event(),
                resourceId,
                event.getData().getOldTunnel(),
                getClass(),
                e,
                DetailedStackStatus.AVAILABLE,
                event.getData().getRevertTime(),
                "Upgrade CCM failed, prerequisites have not met",
                ERROR
        );
    }

    @Override
    protected Selectable doAccept(HandlerEvent<UpgradeCcmEvent> event) {
        UpgradeCcmEvent request = event.getData();
        LOGGER.info("Checking prerequisites for CCM upgrade...");
        upgradeCcmService.checkPrerequsities(request.getResourceId(), request.getOldTunnel());
        return UPGRADE_CCM_CHECK_PREREQUISITES_FINISHED_EVENT.createBasedOn(request);
    }

}
