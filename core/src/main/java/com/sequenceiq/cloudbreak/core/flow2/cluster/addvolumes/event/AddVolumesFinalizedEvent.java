package com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class AddVolumesFinalizedEvent extends StackEvent {

    @JsonCreator
    public AddVolumesFinalizedEvent(@JsonProperty("resourceId") Long resourceId) {
        super(AddVolumesEvent.FINALIZED_EVENT.event(), resourceId);
    }
}