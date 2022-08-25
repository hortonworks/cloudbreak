package com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector;

import com.sequenceiq.flow.event.EventSelectorUtil;

public enum UpgradeCcmStateSelector implements UpgradeCcmFlowEvent {

    UPGRADE_CCM_TRIGGER_EVENT,

    UPGRADE_CCM_CHECK_PREREQUISITES_FINISHED_EVENT,
    UPGRADE_CCM_TUNNEL_CHANGE_FINISHED_EVENT,
    UPGRADE_CCM_OBTAIN_AGENT_DATA_FINISHED_EVENT,
    UPGRADE_CCM_PUSH_SALT_STATES_FINISHED_EVENT,
    UPGRADE_CCM_UPGRADE_FINISHED_EVENT,
    UPGRADE_CCM_RECONFIGURE_NGINX_FINISHED_EVENT,
    UPGRADE_CCM_REGISTER_CLUSTER_PROXY_FINISHED_EVENT,
    UPGRADE_CCM_HEALTH_CHECK_FINISHED_EVENT,
    UPGRADE_CCM_REMOVE_MINA_FINISHED_EVENT,
    UPGRADE_CCM_DEREGISTER_MINA_FINISHED_EVENT,

    UPGRADE_CCM_FINISHED_EVENT,

    UPGRADE_CCM_FAILED_EVENT,
    UPGRADE_CCM_FAILED_REVERT_EVENT,
    UPGRADE_CCM_FAILED_REVERT_ALL_EVENT,
    UPGRADE_CCM_CLEANING_FAILED_EVENT,
    UPGRADE_CCM_FINALIZE_FAILED_EVENT,
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
