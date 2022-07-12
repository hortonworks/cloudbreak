package com.sequenceiq.datalake.flow.datalake.upgrade.preparation;

import com.sequenceiq.datalake.flow.datalake.upgrade.preparation.event.DatalakeUpgradePreparationFailedEvent;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum DatalakeUpgradePreparationEvent  implements FlowEvent {
    DATALAKE_UPGRADE_PREPARATION_TRIGGER_EVENT,
    DATALAKE_UPGRADE_PREPARATION_IN_PROGRESS_EVENT,
    DATALAKE_UPGRADE_PREPARATION_SUCCESS_EVENT,
    DATALAKE_UPGRADE_PREPARATION_FAILED_EVENT(EventSelectorUtil.selector(DatalakeUpgradePreparationFailedEvent.class)),
    DATALAKE_UPGRADE_PREPARATION_FAILED_HANDLED_EVENT,
    DATALAKE_UPGRADE_PREPARATION_FINALIZED_EVENT;

    private final String event;

    DatalakeUpgradePreparationEvent() {
        event = name();
    }

    DatalakeUpgradePreparationEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
