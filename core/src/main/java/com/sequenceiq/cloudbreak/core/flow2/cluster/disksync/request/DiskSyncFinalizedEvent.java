package com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.DiskSyncEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class DiskSyncFinalizedEvent extends StackEvent {

    @JsonCreator
    public DiskSyncFinalizedEvent(@JsonProperty("resourceId") Long resourceId) {
        super(DiskSyncEvent.FINALIZED_EVENT.event(), resourceId, null);
    }
}
