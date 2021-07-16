package com.sequenceiq.datalake.flow.datalake.recovery;

import com.sequenceiq.flow.core.FlowEvent;

public enum DatalakeUpgradeRecoveryEvent implements FlowEvent {

    DATALAKE_RECOVERY_EVENT,
    DATALAKE_RECOVERY_IN_PROGRESS_EVENT,
    DATALAKE_RECOVERY_COULD_NOT_START_EVENT("DatalakeRecoveryCouldNotStartEvent"),
    DATALAKE_RECOVERY_SUCCESS_EVENT("DatalakeRecoverySuccessEvent"),
    DATALAKE_RECOVERY_FAILED_EVENT("DatalakeRecoveryFailedEvent"),
    DATALAKE_RECOVERY_FAILED_HANDLED_EVENT,
    DATALAKE_RECOVERY_FINALIZED_EVENT;

    private final String event;

    DatalakeUpgradeRecoveryEvent(String event) {
        this.event = event;
    }

    DatalakeUpgradeRecoveryEvent() {
        event = name();
    }

    @Override
    public String event() {
        return event;
    }
}
