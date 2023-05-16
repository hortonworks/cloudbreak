package com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.reactor.api.event.BaseFlowEvent;

public class DiskResizeFinishedEvent extends BaseFlowEvent implements Selectable {

    @JsonCreator
    public DiskResizeFinishedEvent(@JsonProperty("resourceId") Long resourceId) {
        super(DiskResizeEvent.DISK_RESIZE_FINISHED_EVENT.event(), resourceId, null);
    }
}

