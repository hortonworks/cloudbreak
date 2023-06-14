package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.scale;

import com.sequenceiq.flow.core.FlowEvent;

public enum ServicesRollingRestartEvent implements FlowEvent {
    SERVICES_ROLLING_RESTART_TRIGGER_EVENT,
    SERVICES_ROLLING_RESTART_IN_PROGRESS_EVENT,
    SERVICES_ROLLING_RESTART_FAILURE_EVENT,
    SERVICES_ROLLING_RESTART_FINISHED_EVENT,
    FINALIZED_EVENT;

    @Override
    public String event() {
        return name();
    }
}
