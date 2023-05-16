package com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.event;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateStateSelectors.DATAHUB_DISK_RESIZE_FINISHED_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.reactor.api.event.BaseFlowEvent;

public class DistroXDiskResizeFinishedEvent extends BaseFlowEvent implements Selectable {

    @JsonCreator
    public DistroXDiskResizeFinishedEvent(@JsonProperty("resourceId") Long resourceId) {
        super(DATAHUB_DISK_RESIZE_FINISHED_EVENT.event(), resourceId, null);
    }
}

