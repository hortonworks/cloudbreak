package com.sequenceiq.cloudbreak.reactor.api.event.stack.consumption;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class AttachedVolumeConsumptionCollectionSchedulingRequest extends StackEvent {

    @JsonCreator
    public AttachedVolumeConsumptionCollectionSchedulingRequest(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }

}
