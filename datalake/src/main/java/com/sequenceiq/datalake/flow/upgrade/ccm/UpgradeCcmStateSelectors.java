package com.sequenceiq.datalake.flow.upgrade.ccm;

import com.sequenceiq.datalake.flow.upgrade.ccm.event.UpgradeCcmFailedEvent;
import com.sequenceiq.datalake.flow.upgrade.ccm.event.UpgradeCcmSuccessEvent;
import com.sequenceiq.flow.core.FlowEvent;

public enum UpgradeCcmStateSelectors implements FlowEvent {

    UPGRADE_CCM_UPGRADE_STACK_EVENT,
    UPGRADE_CCM_SUCCESS_EVENT(UpgradeCcmSuccessEvent.class),
    UPGRADE_CCM_FAILED_EVENT(UpgradeCcmFailedEvent.class),
    UPGRADE_CCM_FAILED_HANDLED_EVENT,
    UPGRADE_CCM_FINALIZED_EVENT;

    private final String event;

    UpgradeCcmStateSelectors() {
        event = name();
    }

    UpgradeCcmStateSelectors(Class<?> eventClass) {
        event = eventClass.getSimpleName();
    }

    @Override
    public String event() {
        return event;
    }

}
