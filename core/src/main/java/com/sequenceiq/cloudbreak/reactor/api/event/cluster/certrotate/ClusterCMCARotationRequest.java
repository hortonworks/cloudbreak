package com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrotate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterCMCARotationRequest extends StackEvent {
    @JsonCreator
    public ClusterCMCARotationRequest(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
