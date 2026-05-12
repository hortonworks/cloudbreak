package com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.DiskSyncEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class DiskSyncProcessFinishedEvent extends StackEvent {

    @JsonCreator
    public DiskSyncProcessFinishedEvent(@JsonProperty("resourceId") Long resourceId) {
        super(DiskSyncEvent.DISK_SYNC_PROCESS_FINISHED_EVENT.event(), resourceId, null);
    }
}
