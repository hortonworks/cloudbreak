package com.sequenceiq.datalake.flow.verticalscale.diskupdate.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum DatalakeDiskUpdateStateSelectors implements FlowEvent {

    DATALAKE_DISK_UPDATE_VALIDATION_EVENT,
    DATALAKE_DISK_UPDATE_VALIDATION_HANDLER_EVENT,
    DATALAKE_DISK_UPDATE_EVENT,
    DATALAKE_DISK_UPDATE_HANDLER_EVENT,
    DATALAKE_DISK_UPDATE_FINISH_EVENT,
    DATALAKE_DISK_UPDATE_FINALIZE_EVENT,
    HANDLED_FAILED_DATALAKE_DISK_UPDATE_EVENT,
    FAILED_DATALAKE_DISK_UPDATE_EVENT;

    @Override
    public String event() {
        return name();
    }
}
