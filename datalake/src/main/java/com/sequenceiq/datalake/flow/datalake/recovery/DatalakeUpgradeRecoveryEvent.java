package com.sequenceiq.datalake.flow.datalake.recovery;

import com.sequenceiq.datalake.flow.datalake.recovery.event.DatalakeRecoveryCouldNotStartEvent;
import com.sequenceiq.datalake.flow.datalake.recovery.event.DatalakeRecoveryFailedEvent;
import com.sequenceiq.datalake.flow.datalake.recovery.event.DatalakeRecoverySuccessEvent;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum DatalakeUpgradeRecoveryEvent implements FlowEvent {

    DATALAKE_RECOVERY_EVENT,
    DATALAKE_RECOVERY_IN_PROGRESS_EVENT,
    DATALAKE_RECOVERY_COULD_NOT_START_EVENT(EventSelectorUtil.selector(DatalakeRecoveryCouldNotStartEvent.class)),
    DATALAKE_RECOVERY_SUCCESS_EVENT(EventSelectorUtil.selector(DatalakeRecoverySuccessEvent.class)),
    DATALAKE_RECOVERY_FAILED_EVENT(EventSelectorUtil.selector(DatalakeRecoveryFailedEvent.class)),
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
