package com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize;

import com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.request.DiskResizeFailedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.request.DiskResizeHandlerRequest;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum DiskResizeEvent implements FlowEvent {
    DISK_RESIZE_TRIGGER_EVENT("DISK_RESIZE_TRIGGER_EVENT"),
    DISK_RESIZE_HANDLER_EVENT(EventSelectorUtil.selector(DiskResizeHandlerRequest.class)),
    DISK_RESIZE_FINISHED_EVENT(EventSelectorUtil.selector(DiskResizeFinishedEvent.class)),
    FINALIZED_EVENT(EventSelectorUtil.selector(DiskResizeFinalizedEvent.class)),
    FAILURE_EVENT("DISK_RESIZE_FAILURE_EVENT"),
    DISK_UPDATE_FAILURE_HANDLED_EVENT(EventSelectorUtil.selector(DiskResizeFailedEvent.class));

    private final String event;

    DiskResizeEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
