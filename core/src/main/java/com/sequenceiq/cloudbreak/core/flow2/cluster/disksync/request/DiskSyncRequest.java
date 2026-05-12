package com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.DiskSyncEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class DiskSyncRequest extends StackEvent {

    @JsonCreator
    public DiskSyncRequest(@JsonProperty("resourceId") Long resourceId) {
        super(DiskSyncEvent.DISK_SYNC_TRIGGER_EVENT.event(), resourceId);
    }
}
