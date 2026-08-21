package com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate;

import com.sequenceiq.flow.core.FlowEvent;

public enum DistroXDiskUpdateStateSelectors implements FlowEvent {
    DATAHUB_DISK_UPDATE_VALIDATION_EVENT("DATAHUB_DISK_UPDATE_VALIDATION_EVENT"),
    DATAHUB_DISK_UPDATE_EVENT("DATAHUB_DISK_UPDATE_EVENT"),
    DATAHUB_DISK_UPDATE_FINISH_EVENT("DATAHUB_DISK_UPDATE_FINISH_EVENT"),
    DATAHUB_DISK_UPDATE_FINALIZE_EVENT("DATAHUB_DISK_UPDATE_FINALIZE_EVENT"),
    HANDLED_FAILED_DATAHUB_DISK_UPDATE_EVENT("HANDLED_FAILED_DATAHUB_DISK_UPDATE_EVENT"),
    FAILED_DATAHUB_DISK_UPDATE_EVENT("FAILED_DATAHUB_DISK_UPDATE_EVENT");

    private final String event;

    DistroXDiskUpdateStateSelectors(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
