package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.recovery.bringup;

import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.recovery.bringup.DatalakeRecoveryRestoreComponentsSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.recovery.bringup.DatalakeRecoverySetupNewInstancesFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.recovery.bringup.DatalakeRecoverySetupNewInstancesSuccess;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum DatalakeRecoveryBringupEvent implements FlowEvent {

    RECOVERY_BRINGUP_EVENT,
    RECOVERY_RESTORE_COMPONENTS_FINISHED_EVENT(EventSelectorUtil.selector(DatalakeRecoveryRestoreComponentsSuccess.class)),
    RECOVERY_BRINGUP_NEW_INSTANCES_FINISHED_EVENT(EventSelectorUtil.selector(DatalakeRecoverySetupNewInstancesSuccess.class)),
    RECOVERY_BRINGUP_FAILED_EVENT(EventSelectorUtil.selector(DatalakeRecoverySetupNewInstancesFailedEvent.class)),
    RECOVERY_BRINGUP_FINALIZED_EVENT,
    RECOVERY_BRINGUP_FAIL_HANDLED_EVENT;

    private final String event;

    DatalakeRecoveryBringupEvent() {
        this.event = name();
    }

    DatalakeRecoveryBringupEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}