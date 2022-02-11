package com.sequenceiq.freeipa.flow.stack.upgrade.ccm.handler;

import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmHandlerSelector.UPGRADE_CCM_CHECK_PREREQUISITES_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_FAILED_EVENT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event.UpgradeCcmCheckPrerequisitesRequest;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event.UpgradeCcmCheckPrerequisitesSuccess;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event.UpgradeCcmFailureEvent;

import reactor.bus.Event;

@Component
public class UpgradeCcmCheckPrerequisitesHandler extends ExceptionCatcherEventHandler<UpgradeCcmCheckPrerequisitesRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeCcmCheckPrerequisitesHandler.class);

    @Override
    public String selector() {
        return UPGRADE_CCM_CHECK_PREREQUISITES_EVENT.event();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpgradeCcmCheckPrerequisitesRequest> event) {
        LOGGER.error("Checking prerequisites for CCM upgrade has failed", e);
        return new UpgradeCcmFailureEvent(UPGRADE_CCM_FAILED_EVENT.event(), resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<UpgradeCcmCheckPrerequisitesRequest> event) {
        UpgradeCcmCheckPrerequisitesRequest request = event.getData();
        LOGGER.info("Checking prerequisites for CCM upgrade...");
        // TODO perform validations
        return new UpgradeCcmCheckPrerequisitesSuccess(request.getResourceId());
    }

}
