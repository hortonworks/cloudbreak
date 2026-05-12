package com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class DiskSyncFailedEvent extends StackFailureEvent {

    @JsonCreator
    public DiskSyncFailedEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("exception") Exception exception) {
        super(selector, resourceId, exception);
    }
}
