package com.sequenceiq.cloudbreak.reactor.api.event.stack.consumption;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class AttachedVolumeConsumptionCollectionUnschedulingRequest extends StackEvent {

    @JsonCreator
    public AttachedVolumeConsumptionCollectionUnschedulingRequest(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }

}
