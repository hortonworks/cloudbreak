package com.sequenceiq.cloudbreak.reactor.api.event.stack.consumption;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class AttachedVolumeConsumptionCollectionSchedulingSuccess extends StackEvent {

    @JsonCreator
    public AttachedVolumeConsumptionCollectionSchedulingSuccess(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }

}
