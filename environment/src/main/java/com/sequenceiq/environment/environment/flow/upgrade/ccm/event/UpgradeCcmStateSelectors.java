package com.sequenceiq.environment.environment.flow.upgrade.ccm.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum UpgradeCcmStateSelectors implements FlowEvent {

    UPGRADE_CCM_VALIDATION_EVENT,
    UPGRADE_CCM_FREEIPA_EVENT,
    UPGRADE_CCM_DATALAKE_EVENT,
    UPGRADE_CCM_DATAHUB_EVENT,
    FINISH_UPGRADE_CCM_EVENT,
    FINALIZE_UPGRADE_CCM_EVENT,
    HANDLED_FAILED_UPGRADE_CCM_EVENT,
    FAILED_UPGRADE_CCM_EVENT;

    @Override
    public String event() {
        return name();
    }
}
