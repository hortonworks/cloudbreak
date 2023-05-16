package com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate;

import com.sequenceiq.flow.core.FlowEvent;

public enum DistroXDiskUpdateHandlerSelectors implements FlowEvent {

    DATAHUB_DISK_RESIZE_HANDLER_EVENT("DATAHUB_DISK_RESIZE_HANDLER_EVENT"),
    DATAHUB_DISK_UPDATE_VALIDATION_HANDLER_EVENT("DATAHUB_DISK_UPDATE_VALIDATION_HANDLER_EVENT"),
    DATAHUB_DISK_UPDATE_HANDLER_EVENT("DATAHUB_DISK_UPDATE_HANDLER_EVENT");

    private final String event;

    DistroXDiskUpdateHandlerSelectors(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
