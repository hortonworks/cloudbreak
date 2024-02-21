package com.sequenceiq.datalake.flow.verticalscale.addvolumes.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum DatalakeAddVolumesStateSelectors implements FlowEvent {

    DATALAKE_ADD_VOLUMES_TRIGGER_EVENT,
    DATALAKE_ADD_VOLUMES_HANDLER_EVENT,
    DATALAKE_ADD_VOLUMES_FINISH_EVENT,
    DATALAKE_ADD_VOLUMES_FINALIZE_EVENT,
    HANDLED_FAILED_DATALAKE_ADD_VOLUMES_EVENT,
    FAILED_DATALAKE_ADD_VOLUMES_EVENT;

    @Override
    public String event() {
        return name();
    }
}