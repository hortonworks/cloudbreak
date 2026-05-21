package com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.DiskSyncEvent;
import com.sequenceiq.cloudbreak.job.disk.DiskSyncMode;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class DiskSyncHandlerEvent extends StackEvent {

    private final DiskSyncMode diskSyncMode;

    @JsonCreator
    public DiskSyncHandlerEvent(@JsonProperty("resourceId") Long resourceId,
        @JsonProperty("diskSyncMode") DiskSyncMode diskSyncMode) {
        super(DiskSyncEvent.DISK_SYNC_HANDLER_EVENT.event(), resourceId, null);
        this.diskSyncMode = diskSyncMode;
    }

    public DiskSyncMode getDiskSyncMode() {
        return diskSyncMode;
    }
}
