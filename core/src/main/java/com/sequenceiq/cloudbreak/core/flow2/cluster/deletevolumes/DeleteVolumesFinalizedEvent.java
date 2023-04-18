package com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class DeleteVolumesFinalizedEvent extends StackEvent {

    @JsonCreator
    public DeleteVolumesFinalizedEvent(
            @JsonProperty("resourceId") Long resourceId) {
        super(DeleteVolumesEvent.FINALIZED_EVENT.event(), resourceId);
    }
}
