package com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskSyncMode;
import com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.DiskSyncEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class DiskSyncRequest extends StackEvent {

    private final DiskSyncMode diskSyncMode;

    @JsonCreator
    public DiskSyncRequest(@JsonProperty("resourceId") Long resourceId,
        @JsonProperty("diskSyncMode") DiskSyncMode diskSyncMode) {
        super(DiskSyncEvent.DISK_SYNC_TRIGGER_EVENT.event(), resourceId);
        this.diskSyncMode = diskSyncMode;
    }

    public DiskSyncMode getDiskSyncMode() {
        return diskSyncMode;
    }
}
