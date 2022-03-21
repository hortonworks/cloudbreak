package com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector;

import com.sequenceiq.flow.event.EventSelectorUtil;

public enum UpgradeCcmHandlerSelector implements UpgradeCcmFlowEvent {

    UPGRADE_CCM_CHECK_PREREQUISITES_EVENT,
    UPGRADE_CCM_CHANGE_TUNNEL_EVENT,
    UPGRADE_CCM_PUSH_SALT_STATES_EVENT,
    UPGRADE_CCM_UPGRADE_EVENT,
    UPGRADE_CCM_RECONFIGURE_EVENT,
    UPGRADE_CCM_REGISTER_CCM_EVENT,
    UPGRADE_CCM_HEALTH_CHECK_EVENT,
    UPGRADE_CCM_REMOVE_MINA_EVENT;

    private final String event;

    UpgradeCcmHandlerSelector(Class<?> aClass) {
        this.event = EventSelectorUtil.selector(aClass);
    }

    UpgradeCcmHandlerSelector() {
        this.event = name();
    }

    @Override
    public String event() {
        return event;
    }
}
