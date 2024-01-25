package com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class DiskResizeFinishedEvent extends StackEvent {

    @JsonCreator
    public DiskResizeFinishedEvent(@JsonProperty("resourceId") Long resourceId) {
        super(DiskResizeEvent.DISK_RESIZE_FINISHED_EVENT.event(), resourceId, null);
    }
}

