package com.sequenceiq.freeipa.flow.stack.upgrade.ccm.handler;

import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmHandlerSelector.UPGRADE_CCM_HEALTH_CHECK_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_FAILED_REVERT_ALL_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_HEALTH_CHECK_FINISHED_EVENT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event.UpgradeCcmEvent;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event.UpgradeCcmFailureEvent;

import reactor.bus.Event;

@Component
public class UpgradeCcmHealthCheckHandler extends AbstractUpgradeCcmEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeCcmHealthCheckHandler.class);

    @Override
    public String selector() {
        return UPGRADE_CCM_HEALTH_CHECK_EVENT.event();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpgradeCcmEvent> event) {
        LOGGER.error("Health check for CCM upgrade has failed", e);
        return new UpgradeCcmFailureEvent(UPGRADE_CCM_FAILED_REVERT_ALL_EVENT.event(), resourceId, event.getData().getOldTunnel(), getClass(), e,
                event.getData().getRevertTime(), "After upgrading CCM health check has failed.");
    }

    @Override
    protected Selectable doAccept(HandlerEvent<UpgradeCcmEvent> event) {
        // kept for forward compatibility with CB-14585
        UpgradeCcmEvent request = event.getData();
        return UPGRADE_CCM_HEALTH_CHECK_FINISHED_EVENT.createBasedOn(request);
    }

}
