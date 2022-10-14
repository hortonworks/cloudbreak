package com.sequenceiq.cloudbreak.reactor.api.event.stack.consumption;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class AttachedVolumeConsumptionCollectionSchedulingFailed extends StackFailureEvent {

    @JsonCreator
    public AttachedVolumeConsumptionCollectionSchedulingFailed(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception exception) {
        super(stackId, exception);
    }

}
