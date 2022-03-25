package com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector;

import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event.UpgradeCcmFailureEvent;

public enum UpgradeCcmStateSelector implements UpgradeCcmFlowEvent {

    UPGRADE_CCM_TRIGGER_EVENT,

    UPGRADE_CCM_CHECK_PREREQUISITES_FINISHED_EVENT,
    UPGRADE_CCM_TUNNEL_CHANGE_FINISHED_EVENT,
    UPGRADE_CCM_PUSH_SALT_STATES_FINISHED_EVENT,
    UPGRADE_CCM_UPGRADE_FINISHED_EVENT,
    UPGRADE_CCM_RECONFIGURE_FINISHED_EVENT,
    UPGRADE_CCM_REGISTER_CCM_FINISHED_EVENT,
    UPGRADE_CCM_HEALTH_CHECK_FINISHED_EVENT,
    UPGRADE_CCM_REMOVE_MINA_FINISHED_EVENT,

    UPGRADE_CCM_FINISHED_EVENT,
    UPGRADE_CCM_FAILED_EVENT(UpgradeCcmFailureEvent.class),
    UPGRADE_CCM_FAILURE_HANDLED_EVENT;

    private final String event;

    UpgradeCcmStateSelector() {
        this.event = name();
    }

    UpgradeCcmStateSelector(Class<?> aClass) {
        this.event = EventSelectorUtil.selector(aClass);
    }

    @Override
    public String event() {
        return event;
    }
}
