package com.sequenceiq.environment.environment.flow.upgrade.ccm.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum UpgradeCcmHandlerSelectors implements FlowEvent {

    UPGRADE_CCM_VALIDATION_HANDLER,
    UPGRADE_CCM_FREEIPA_HANDLER,
    UPGRADE_CCM_DATALAKE_HANDLER,
    UPGRADE_CCM_DATAHUB_HANDLER;

    @Override
    public String event() {
        return name();
    }
}
