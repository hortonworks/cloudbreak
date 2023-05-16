package com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.reactor.api.event.BaseFlowEvent;

public class DiskResizeFinalizedEvent extends BaseFlowEvent implements Selectable {

    @JsonCreator
    public DiskResizeFinalizedEvent(@JsonProperty("resourceId") Long resourceId) {
        super(DiskResizeEvent.FINALIZED_EVENT.event(), resourceId, null);
    }
}
