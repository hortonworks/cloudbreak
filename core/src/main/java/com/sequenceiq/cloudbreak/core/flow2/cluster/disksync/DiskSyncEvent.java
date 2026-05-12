package com.sequenceiq.cloudbreak.core.flow2.cluster.disksync;

import com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.request.DiskSyncFailedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.request.DiskSyncFinalizedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.request.DiskSyncHandlerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.request.DiskSyncProcessFinishedEvent;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum DiskSyncEvent implements FlowEvent {
    DISK_SYNC_TRIGGER_EVENT("DISK_SYNC_TRIGGER_EVENT"),
    DISK_SYNC_HANDLER_EVENT(EventSelectorUtil.selector(DiskSyncHandlerEvent.class)),
    DISK_SYNC_PROCESS_FINISHED_EVENT(EventSelectorUtil.selector(DiskSyncProcessFinishedEvent.class)),
    FINALIZED_EVENT(EventSelectorUtil.selector(DiskSyncFinalizedEvent.class)),
    FAILURE_EVENT(EventSelectorUtil.selector(DiskSyncFailedEvent.class)),
    DISK_SYNC_FAILURE_HANDLED_EVENT("DISK_SYNC_FAILURE_HANDLED_EVENT");

    private final String event;

    DiskSyncEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
