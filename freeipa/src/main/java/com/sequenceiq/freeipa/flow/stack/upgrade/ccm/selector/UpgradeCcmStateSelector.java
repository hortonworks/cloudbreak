package com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event.UpgradeCcmFailureEvent;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event.UpgradeCcmCheckPrerequisitesSuccess;

public enum UpgradeCcmStateSelector implements FlowEvent {

    UPGRADE_CCM_TRIGGER_EVENT,

    UPGRADE_CCM_CHECK_PREREQUISITES_FINISHED_EVENT(EventSelectorUtil.selector(UpgradeCcmCheckPrerequisitesSuccess.class)),
    // TODO handler success events

    UPGRADE_CCM_FINISHED_EVENT,
    UPGRADE_CCM_FAILED_EVENT(EventSelectorUtil.selector(UpgradeCcmFailureEvent.class)),
    UPGRADE_CCM_FAILURE_HANDLED_EVENT;

    private final String event;

    UpgradeCcmStateSelector(String event) {
        this.event = event;
    }

    UpgradeCcmStateSelector() {
        this.event = name();
    }

    @Override
    public String event() {
        return event;
    }

}
